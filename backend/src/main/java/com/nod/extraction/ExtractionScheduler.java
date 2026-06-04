package com.nod.extraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nod.card.NodCard;
import com.nod.card.NodCardRepository;
import com.nod.card.NodCardStatus;
import com.nod.gmail.GmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

@Component
public class ExtractionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExtractionScheduler.class);

    private final GmailService gmailService;
    private final GeminiClient geminiClient;
    private final NodCardRepository nodCardRepository;
    private final ObjectMapper objectMapper;
    private final JdbcClient jdbcClient;

    public ExtractionScheduler(GmailService gmailService, GeminiClient geminiClient,
            NodCardRepository nodCardRepository, JdbcClient jdbcClient) {
        this.gmailService = gmailService;
        this.geminiClient = geminiClient;
        this.nodCardRepository = nodCardRepository;
        this.jdbcClient = jdbcClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Scheduled pipeline execution. Runs hourly.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void runExtractionPipelineScheduled() {
        log.info("Triggered scheduled chore extraction pipeline");
        // Simulated access token / user processing.
        // In full production, this would query active users from database with valid
        // refresh tokens.
    }

    /**
     * Runs the pipeline for a specific user and access token.
     * Ephemerally fetches emails, queries Gemini, and writes chores to PostgreSQL.
     */
    public int runPipelineForUser(String userId, String accessToken) throws Exception {
        log.info("Starting extraction pipeline for user {}", userId);

        ensureUserExists(userId, accessToken);

        // Fetch emails in-memory (ephemeral)
        // Fetch last 20 emails only - 1 Gemini call to minimize token usage on free
        // tier
        List<GmailService.EphemeralEmail> emails = gmailService.fetchRecentEmails(accessToken, 20);

        if (emails.isEmpty()) {
            log.info("No emails retrieved for user {}", userId);
            return 0;
        }

        // Process all 20 emails in a single Gemini call using ONLY subject+sender (no
        // body)
        // This reduces token usage by ~90% and stays well within free tier
        int totalCount = 0;
        StringBuilder textBuilder = new StringBuilder();
        for (var email : emails) {
            textBuilder.append("From: ").append(email.sender()).append("\n")
                    .append("Subject: ").append(email.subject()).append("\n")
                    .append("----------------------------------------\n");
        }

        log.info("Processing {} emails in a single Gemini call for user {}", emails.size(), userId);

        try {
            String jsonResult = geminiClient.extractChores(textBuilder.toString());
            JsonNode rootNode = objectMapper.readTree(jsonResult);

            if (rootNode.isArray()) {
                for (JsonNode item : rootNode) {
                    String choreType = item.path("chore_type").asText("general_chore");
                    String summaryText = item.path("summary_text").asText("Actionable Task");
                    JsonNode payloadNode = item.path("action_payload");
                    String actionPayloadStr = objectMapper.writeValueAsString(payloadNode);

                    NodCard card = new NodCard(
                            UUID.randomUUID().toString(),
                            userId,
                            choreType,
                            summaryText,
                            NodCardStatus.pending,
                            actionPayloadStr,
                            Instant.now(),
                            Instant.now());

                    nodCardRepository.save(card);
                    totalCount++;
                }
            }
        } catch (Exception e) {
            log.error("Gemini extraction failed: {}", e.getMessage());
        }

        log.info("Successfully extracted and saved {} chores for user {}", totalCount, userId);
        return totalCount;
    }

    private void ensureUserExists(String userId, String accessToken) {
        var countOpt = jdbcClient.sql("SELECT count(*) FROM users WHERE id = ?")
                .param(userId)
                .query(Integer.class)
                .optional();
        int count = countOpt.orElse(0);

        if (count == 0) {
            log.info("User {} not found in DB. Auto-provisioning from Gmail profile...", userId);
            try {
                String email = gmailService.fetchUserProfile(accessToken);
                jdbcClient.sql("INSERT INTO users (id, email, display_name) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING")
                        .param(userId)
                        .param(email)
                        .param("Auto-Provisioned User")
                        .update();
                log.info("User {} successfully provisioned.", userId);
            } catch (Exception e) {
                log.error("Failed to auto-provision user {}: {}", userId, e.getMessage());
            }
        }
    }
}
