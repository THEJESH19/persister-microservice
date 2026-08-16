package com.skylo.streaming.persister.controller;

import com.skylo.streaming.persister.dto.MessageRequest;
import com.skylo.streaming.persister.dto.PersistStatus;
import com.skylo.streaming.persister.service.MessagePersistenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing REST endpoints for message persistence.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Message Persistence", description = "Endpoints for idempotent persistence of messages")
public class MessagePersisterController {

    private final MessagePersistenceService persistenceService;

    /**
     * Persists a message. Returns 201 Created for new messages, and 208 Already Reported for duplicates.
     *
     * @param request the incoming message payload
     * @return response entity with corresponding HTTP status
     */
    @PostMapping("/persist-message")
    @Operation(summary = "Persist Message", description = "Receives a message from the consumer and persists it idempotently.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Message successfully persisted (first-time receipt)",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "208", description = "Duplicate message detected and skipped (already processed)",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request payload supplied",
                     content = @Content)
    })
    public ResponseEntity<String> persistMessage(@Valid @RequestBody MessageRequest request) {
        log.info("Received request to persist message ID: {}", request.getId());

        PersistStatus status = persistenceService.persist(request);

        if (status == PersistStatus.CREATED) {
            return ResponseEntity.status(HttpStatus.CREATED).body("Message persisted successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.ALREADY_REPORTED).body("Message already processed (idempotent skip).");
        }
    }
}
