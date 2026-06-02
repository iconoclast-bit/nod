package com.nod.card;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class NodCardRepository {
    private final JdbcClient jdbcClient;

    public NodCardRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Finds a NodCard by its ID and User ID.
     */
    public Optional<NodCard> findByIdAndUserId(String id, String userId) {
        return jdbcClient.sql("""
                    SELECT id, user_id, chore_type, summary_text, status, action_payload, created_at, updated_at
                    FROM nod_cards
                    WHERE id = :id AND user_id = :userId
                """)
                .param("id", id)
                .param("userId", userId)
                .query((rs, rowNum) -> new NodCard(
                        rs.getString("id"),
                        rs.getString("user_id"),
                        rs.getString("chore_type"),
                        rs.getString("summary_text"),
                        NodCardStatus.valueOf(rs.getString("status")),
                        rs.getString("action_payload"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()
                ))
                .optional();
    }

    /**
     * Updates the status of a NodCard for a specific user.
     */
    public int updateStatus(String id, String userId, NodCardStatus status) {
        return jdbcClient.sql("""
                    UPDATE nod_cards
                    SET status = :status, updated_at = :updatedAt
                    WHERE id = :id AND user_id = :userId
                """)
                .param("status", status.name())
                .param("updatedAt", Instant.now())
                .param("id", id)
                .param("userId", userId)
                .update();
    }

    /**
     * Saves a new NodCard to the database.
     */
    public int save(NodCard card) {
        return jdbcClient.sql("""
                    INSERT INTO nod_cards (id, user_id, chore_type, summary_text, status, action_payload, created_at, updated_at)
                    VALUES (:id, :userId, :choreType, :summaryText, :status, CAST(:actionPayload AS jsonb), :createdAt, :updatedAt)
                """)
                .param("id", card.id())
                .param("userId", card.userId())
                .param("choreType", card.choreType())
                .param("summaryText", card.summaryText())
                .param("status", card.status().name())
                .param("actionPayload", card.actionPayload())
                .param("createdAt", card.createdAt() != null ? card.createdAt() : Instant.now())
                .param("updatedAt", card.updatedAt() != null ? card.updatedAt() : Instant.now())
                .update();
    }
}
