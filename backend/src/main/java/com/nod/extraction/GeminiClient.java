package com.nod.extraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.LinkedList;
import java.util.Queue;

@Component
public class GeminiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Sliding window for rate limiting
    private final Queue<Long> requestTimestamps = new LinkedList<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 4; // 1 below the 5 RPM limit

    @Value("${gemini.api.key:MOCK_KEY}")
    private String apiKey;

    public GeminiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Enforces the 5 RPM limit by pausing the thread if we've sent too many requests.
     */
    private synchronized void checkAndEnforceRateLimit() throws InterruptedException {
        long now = System.currentTimeMillis();
        // Clear timestamps older than 1 minute
        while (!requestTimestamps.isEmpty() && now - requestTimestamps.peek() > 60000) {
            requestTimestamps.poll();
        }

        if (requestTimestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            long oldest = requestTimestamps.peek();
            long waitTime = 60000 - (now - oldest);
            if (waitTime > 0) {
                // Pause thread until the oldest request falls out of the 1-minute window
                Thread.sleep(waitTime);
            }
        }

        requestTimestamps.add(System.currentTimeMillis());
    }

    /**
     * Sends email content to Gemini for chore extraction.
     * Returns a JSON array string matching the nod_card spec.
     */
    public String extractChores(String emailsText) throws IOException, InterruptedException {
        if ("MOCK_KEY".equals(apiKey) || apiKey.isBlank()) {
            // Fallback mock payload for testing if API key is not present
            return """
                [
                  {
                    "chore_type": "cancel_subscription",
                    "summary_text": "Cancel Spotify Premium Subscription",
                    "action_payload": {
                      "service": "Spotify",
                      "action": "cancel",
                      "url": "https://www.spotify.com/us/account/overview/"
                    }
                  }
                ]
                """;
        }

        String systemInstruction = """
                You are an expert data extraction agent for Indian users. Analyze the provided emails and extract ALL chores, action items, or time-sensitive tasks.
                Be VERY aggressive in detecting actionable items. Identify events such as:
                1. Credit card payment reminders or due dates (CRED, HDFC, SBI, ICICI, Axis, Kotak, etc.)
                2. Loan EMI or insurance premium payment reminders
                3. Utility bill payments (electricity, gas, internet, mobile recharge)
                4. Subscription renewals or updates (Spotify, Netflix, Amazon Prime, Hotstar, etc.)
                5. Flight check-ins or travel bookings requiring action
                6. Product returns, exchange deadlines from Amazon, Flipkart, Myntra, Meesho, etc.
                7. Bank alerts requiring action (OTP expiry, KYC update, account verification)
                8. Meeting invites, appointment reminders, deadlines
                9. Tax filing or compliance reminders
                10. Any email with words like "due", "reminder", "pay now", "action required", "expires", "last date", "don't miss", "urgent"

                For each chore identified, output a JSON object inside a JSON array. Each object MUST have:
                - chore_type: A short snake_case category (e.g. credit_card_payment, emi_payment, bill_payment, subscription_renewal, flight_checkin, product_return, kyc_update, tax_filing)
                - summary_text: A concise, human-readable description (e.g. "Pay CRED credit card bill by June 5", "Renew Jio plan by June 10", "Check-in for IndiGo flight 6E-123")
                - action_payload: A JSON object with execution details: {"url": "...", "service": "...", "amount": "...", "due_date": "..."}

                If no chores are found, return an empty JSON array: [].
                IMPORTANT: Respond ONLY with a valid JSON array. No markdown, no explanation, no code blocks.
                """;

        // Build request body - no generationConfig needed; we instruct via prompt
        Map<String, Object> requestMap = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", "System Instruction:\n" + systemInstruction + "\n\nEmails to process:\n" + emailsText + "\n\nIMPORTANT: Respond ONLY with a valid JSON array. No markdown, no explanation, no code blocks. Just the raw JSON array starting with [ and ending with ].")
                        })
                }
        );

        String requestBody = objectMapper.writeValueAsString(requestMap);
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(60))
                .build();

        // Enforce rate limits BEFORE sending request
        checkAndEnforceRateLimit();

        // Retry with exponential backoff on 429 quota errors and 503 high demand errors (max 3 attempts)
        HttpResponse<String> response = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 429 && response.statusCode() != 503) break;
            if (attempt < 3) {
                long waitMs = (long) Math.pow(2, attempt) * 5000L; // 10s, 20s
                Thread.sleep(waitMs);
            }
        }

        if (response.statusCode() != 200) {
            throw new IOException("Gemini API returned error code " + response.statusCode() + ": " + response.body());
        }

        // Parse response to retrieve the JSON text
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode partsNode = root.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0);

        if (partsNode.isMissingNode()) {
            throw new IOException("Unexpected response format from Gemini: " + response.body());
        }

        String responseText = partsNode.path("text").asText().trim();

        // Safety net: strip markdown code fences if Gemini wraps response in ```json ... ```
        if (responseText.startsWith("```")) {
            responseText = responseText.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
        }

        return responseText;
    }
}
