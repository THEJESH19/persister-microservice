package com.skylo.streaming.persister.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object representing the message payload received by the persister.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRequest {

    @NotBlank(message = "Message ID is required")
    private String id;

    @NotBlank(message = "Message Key is required")
    private String key;

    @NotBlank(message = "Message Payload is required")
    private String payload;

    @NotNull(message = "Message Timestamp is required")
    private Instant timestamp;
}
