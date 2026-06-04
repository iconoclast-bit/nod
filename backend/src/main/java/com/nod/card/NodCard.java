package com.nod.card;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NodCard(
    String id,
    @JsonProperty("user_id") String userId,
    @JsonProperty("chore_type") String choreType,
    @JsonProperty("summary_text") String summaryText,
    NodCardStatus status,
    @JsonProperty("action_payload") String actionPayload,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("updated_at") Instant updatedAt
) {}
