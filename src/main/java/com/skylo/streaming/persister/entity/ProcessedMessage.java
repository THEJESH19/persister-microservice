package com.skylo.streaming.persister.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing a processed message persisted in the PostgreSQL database.
 * The message ID serves as the primary key to prevent duplicate processing.
 */
@Entity
@Table(name = "processed_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedMessage {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "message_key", nullable = false)
    private String messageKey;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "message_timestamp", nullable = false)
    private Instant messageTimestamp;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
