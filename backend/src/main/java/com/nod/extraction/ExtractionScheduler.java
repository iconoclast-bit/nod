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

@Component
public class ExtractionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExtractionScheduler.class);

    private final GmailService gmailService;
    private final GeminiClient geminiClient;
    private final NodCardRepository nodCardRepository;
    private final ObjectMapper objectMapper;

    public ExtractionScheduler(GmailService gmailService, GeminiClient geminiClient,
                               NodCardRepository nodCardRepository) {
        this.gmailService = gmailService;
        this.geminiClient = geminiClient;
        this.nodCardRepository = nodCardRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Scheduled pipeline execution. Runs hourly.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void runExtractionPipelineScheduled() {
        log.info("Triggered scheduled chore extraction pipeline");
        // Simulated access token / user processing.
        // In full production, this would query active users from database with valid refresh tokens.
    }

    /**
     * Runs the pipeline for a specific user and access token.
     * Ephemerally fetches emails, queries Gemini, and writes chores to PostgreSQL.
     */
    public int runPipelineForUser(String userId, String accessToken) throws Exception {
        log.info("Starting extraction pipeline for user {}", userId);

        // Fetch emails in-memory (ephemeral)
        List<GmailService.EphemeralEmail> emails = gmailService.fetchRecentEmails(accessToken, 50);

        if (emails.isEmpty()) {
            log.info("No emails retrieved for user {}", userId);
            return 0;
        }

        // Format emails into single prompt context
        StringBuilder textBuilder = new StringBuilder();
        for (var email : emails) {
            textBuilder.append("From: ").append(email.sender()).append("\n")
                    .append("Subject: ").append(email.subject()).append("\n")
                    .append("Body:\n").append(email.body()).append("\n")
                    .append("----------------------------------------\n");
        }

        // Call Gemini
        String jsonResult = geminiClient.extractChores(textBuilder.toString());

        // Parse extracted output and save as pending cards
        JsonNode rootNode = objectMapper.readTree(jsonResult);
        int count = 0;

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
                        Instant.now()
                );

                nodCardRepository.save(card);
                count++;
            }
        }

        log.info("Successfully extracted and saved {} chores for user {}", count, userId);
        return count;
    }
}
