package com.nod.card;

import java.time.Instant;

public record NodCard(
    String id,
    String userId,
    String choreType,
    String summaryText,
    NodCardStatus status,
    String actionPayload, // Store JSONB content as a raw JSON string
    Instant createdAt,
    Instant updatedAt
) {}
