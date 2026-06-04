package com.nod.gmail;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GmailService {

    private static final Logger log = LoggerFactory.getLogger(GmailService.class);

    public record EphemeralEmail(String id, String subject, String body, String sender, String receivedDate) {}

    /**
     * Fetches the user's profile email address using their accessToken.
     */
    public String fetchUserProfile(String accessToken) throws GeneralSecurityException, IOException {
        Gmail service = new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                request -> {
                    String authHeader = "Bearer " + accessToken;
                    request.getHeaders().setAuthorization(authHeader);
                }
        )
                .setApplicationName("Nod")
                .build();
        
        com.google.api.services.gmail.model.Profile profile = service.users().getProfile("me").execute();
        return profile.getEmailAddress();
    }

    /**
     * Fetches recent messages using a user's accessToken.
     * Ephemeral: processes and returns email representation in-memory.
     */
    public List<EphemeralEmail> fetchRecentEmails(String accessToken, int maxResults) throws GeneralSecurityException, IOException {
        Gmail service = new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                request -> {
                    String authHeader = "Bearer " + accessToken;
                    log.info("Setting Gmail API Authorization header: '{}'", authHeader);
                    request.getHeaders().setAuthorization(authHeader);
                }
        )
                .setApplicationName("Nod")
                .build();

        // Retrieve messages metadata list - filter to last 30 days to get recent bills/reminders
        ListMessagesResponse listResponse = service.users().messages().list("me")
                .setMaxResults((long) maxResults)
                .setQ("newer_than:30d")
                .execute();

        List<Message> messages = listResponse.getMessages();
        List<EphemeralEmail> results = new ArrayList<>();

        if (messages != null) {
            for (Message msgMeta : messages) {
                // Fetch full message details
                Message message = service.users().messages().get("me", msgMeta.getId()).execute();
                
                String id = message.getId();
                String snippet = message.getSnippet(); // short text snippet
                String body = getMessageBody(message); // full text if available
                if (body == null || body.isBlank()) {
                    body = snippet;
                }

                String subject = "";
                String sender = "";
                String date = "";

                if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
                    for (var header : message.getPayload().getHeaders()) {
                        if ("Subject".equalsIgnoreCase(header.getName())) {
                            subject = header.getValue();
                        } else if ("From".equalsIgnoreCase(header.getName())) {
                            sender = header.getValue();
                        } else if ("Date".equalsIgnoreCase(header.getName())) {
                            date = header.getValue();
                        }
                    }
                }

                results.add(new EphemeralEmail(id, subject, body, sender, date));
            }
        }

        return results;
    }

    private String getMessageBody(Message message) {
        if (message.getPayload() == null) return "";
        // If message is simple text
        if (message.getPayload().getBody() != null && message.getPayload().getBody().getData() != null) {
            return new String(com.google.api.client.util.Base64.decodeBase64(message.getPayload().getBody().getData()));
        }
        // If message is multipart, parse parts
        if (message.getPayload().getParts() != null) {
            StringBuilder sb = new StringBuilder();
            for (var part : message.getPayload().getParts()) {
                if ("text/plain".equalsIgnoreCase(part.getMimeType()) && part.getBody() != null && part.getBody().getData() != null) {
                    sb.append(new String(com.google.api.client.util.Base64.decodeBase64(part.getBody().getData())));
                }
            }
            return sb.toString();
        }
        return "";
    }
}
