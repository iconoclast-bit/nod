package com.nod.card;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class NodCardController {

    private final NodCardRepository nodCardRepository;
    private final com.nod.extraction.ExtractionScheduler extractionScheduler;

    public NodCardController(NodCardRepository nodCardRepository,
            com.nod.extraction.ExtractionScheduler extractionScheduler) {
        this.nodCardRepository = nodCardRepository;
        this.extractionScheduler = extractionScheduler;
    }

    public record StatusUpdateRequest(String status) {
    }

    @PatchMapping("/users/{userId}/nod-cards/{cardId}")
    public ResponseEntity<?> updateCardStatus(
            @PathVariable String userId,
            @PathVariable String cardId,
            @RequestBody StatusUpdateRequest request) {
        if (request.status() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status field is required"));
        }

        NodCardStatus newStatus;
        try {
            newStatus = NodCardStatus.valueOf(request.status().toLowerCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value"));
        }

        // Check if card exists
        var cardOpt = nodCardRepository.findByIdAndUserId(cardId, userId);
        if (cardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NodCard not found for the given user"));
        }

        // Perform update
        int rowsUpdated = nodCardRepository.updateStatus(cardId, userId, newStatus);
        if (rowsUpdated == 0) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update card status"));
        }

        return ResponseEntity.ok(Map.of(
                "message", "NodCard status updated successfully",
                "cardId", cardId,
                "status", newStatus.name()));
    }

    @GetMapping("/users/{userId}/nod-cards")
    public ResponseEntity<?> getPendingCards(
            @PathVariable String userId,
            @RequestParam(required = false, defaultValue = "pending") String status) {
        NodCardStatus cardStatus;
        try {
            cardStatus = NodCardStatus.valueOf(status.toLowerCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value"));
        }

        java.util.List<NodCard> cards = nodCardRepository.findByUserIdAndStatus(userId, cardStatus);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .body(cards);
    }

    @GetMapping("/auth/google")
    public Object getGoogleAuthUrl(@RequestParam(required = false, defaultValue = "false") boolean redirect) {
        // Boilerplate Google OAuth Url generation
        String clientId = System.getenv().getOrDefault("GOOGLE_CLIENT_ID", "MOCK_CLIENT_ID");
        String redirectUri = System.getenv().getOrDefault("GOOGLE_REDIRECT_URI",
                "http://localhost:8080/api/auth/google/callback");
        String scopes = "https://www.googleapis.com/auth/gmail.readonly https://www.googleapis.com/auth/calendar.readonly";

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=" + scopes +
                "&access_type=offline" +
                "&prompt=consent";

        if (redirect) {
            RedirectView redirectView = new RedirectView();
            redirectView.setUrl(authUrl);
            return redirectView;
        }

        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    @GetMapping("/auth/google/callback")
    public ResponseEntity<?> googleAuthCallback(@RequestParam(required = false) String code) {
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Authorization code is missing"));
        }

        // Boilerplate for code exchange.
        return ResponseEntity.ok(Map.of(
                "message", "Authentication successful",
                "tokens", Map.of(
                        "access_token", "mock_access_token_abc123",
                        "refresh_token", "mock_refresh_token_xyz789",
                        "expires_in", 3600,
                        "token_type", "Bearer")));
    }

    @PostMapping("/users/{userId}/extract")
    public ResponseEntity<?> triggerExtraction(
            @PathVariable String userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        System.out.println(">>> RECEIVED EXTRACTION REQUEST FOR USER: " + userId);
        System.out.println(">>> RECEIVED AUTH HEADER: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println(">>> REJECTING REQUEST: Missing or invalid Authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7);

        try {
            int extractedCount = extractionScheduler.runPipelineForUser(userId, token);
            return ResponseEntity.ok(Map.of(
                    "message", "Extraction completed successfully",
                    "userId", userId,
                    "extractedCount", extractedCount));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Extraction pipeline failed", "details", e.getMessage()));
        }
    }
}
