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

@Component
public class GeminiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:MOCK_KEY}")
    private String apiKey;

    public GeminiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
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
                You are an expert data extraction agent. Analyze the provided emails and extract chores or actionable tasks.
                Identify events such as:
                1. Subscription renewals or updates (e.g. canceling Spotify, Netflix, etc.)
                2. Flight check-ins
                3. Product return windows
                4. Scheduled payments or utility bill events

                For each chore identified, output a JSON object inside a JSON array. Each object MUST have the following keys:
                - chore_type: A short snake_case category of the chore (e.g. cancel_subscription, flight_checkin, product_return)
                - summary_text: A concise text description of the chore (e.g. "Cancel Spotify Premium", "Check in for flight UA123", "Return product to Amazon by June 12")
                - action_payload: A JSON object containing specific execution details for a Puppeteer agent (e.g. {"url": "https://spotify.com", "service": "Spotify", "due_date": "2026-06-15"}).

                If no chores are found, return an empty JSON array: [].
                """;

        // Build request body manually or via map
        Map<String, Object> requestMap = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", "System Instruction:\n" + systemInstruction + "\n\nEmails to process:\n" + emailsText)
                        })
                },
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );

        String requestBody = objectMapper.writeValueAsString(requestMap);
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

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

        return partsNode.path("text").asText();
    }
}
