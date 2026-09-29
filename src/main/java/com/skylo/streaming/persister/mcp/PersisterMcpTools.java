package com.skylo.streaming.persister.mcp;

import com.skylo.streaming.persister.dto.MessageRequest;
import com.skylo.streaming.persister.dto.PersistStatus;
import com.skylo.streaming.persister.repository.ProcessedMessageRepository;
import com.skylo.streaming.persister.service.MessagePersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedded MCP Tools for Skylo Persister Microservice (Option C: Spring AI).
 * Directly exposes persistence and API documentation capabilities as structured MCP tools over SSE.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PersisterMcpTools {

    private final MessagePersistenceService persistenceService;
    private final ProcessedMessageRepository repository;

    /**
     * MCP Tool: Persist a message idempotently.
     */
    @Tool(description = "Persists a streaming message idempotently into PostgreSQL. Returns CREATED for first-time persistence, or ALREADY_REPORTED if the message was already processed.")
    public Map<String, Object> persistMessage(
            @ToolParam(description = "Unique message ID (UUID or custom identifier)", required = true) String id,
            @ToolParam(description = "Message partition key (e.g. device ID, stream key)", required = true) String key,
            @ToolParam(description = "Message payload content", required = true) String payload,
            @ToolParam(description = "ISO-8601 UTC timestamp string (optional, defaults to now)", required = false) String timestamp
    ) {
        log.info("[MCP Tool] Executing persistMessage for ID: {}, Key: {}", id, key);

        Instant parsedTimestamp;
        if (timestamp != null && !timestamp.trim().isEmpty()) {
            try {
                parsedTimestamp = Instant.parse(timestamp.trim());
            } catch (DateTimeParseException e) {
                log.warn("[MCP Tool] Invalid timestamp provided: {}. Defaulting to Instant.now()", timestamp);
                parsedTimestamp = Instant.now();
            }
        } else {
            parsedTimestamp = Instant.now();
        }

        MessageRequest request = MessageRequest.builder()
                .id(id)
                .key(key)
                .payload(payload)
                .timestamp(parsedTimestamp)
                .build();

        PersistStatus status = persistenceService.persist(request);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("id", id);
        result.put("key", key);

        if (status == PersistStatus.CREATED) {
            result.put("httpStatus", 201);
            result.put("status", "CREATED");
            result.put("message", "Message successfully persisted to PostgreSQL.");
        } else {
            result.put("httpStatus", 208);
            result.put("status", "ALREADY_REPORTED");
            result.put("message", "Duplicate message detected and skipped (idempotent operation).");
        }

        return result;
    }

    /**
     * MCP Tool: Get Persister API Documentation as structured JSON.
     */
    @Tool(description = "Retrieves structured API documentation, endpoint signatures, and schema specs for the persister microservice.")
    public Map<String, Object> getPersisterApiDocs() {
        log.info("[MCP Tool] Executing getPersisterApiDocs");

        Map<String, Object> docs = new LinkedHashMap<>();
        docs.put("service", "persister-microservice");
        docs.put("version", "1.0.0");
        docs.put("port", 8082);
        docs.put("baseUrl", "http://localhost:8082");

        Map<String, Object> persistEndpoint = new LinkedHashMap<>();
        persistEndpoint.put("path", "/api/v1/persist-message");
        persistEndpoint.put("method", "POST");
        persistEndpoint.put("summary", "Idempotent message persistence");
        persistEndpoint.put("description", "Receives a message from the Kafka consumer pipeline and writes it to PostgreSQL.");
        persistEndpoint.put("requestBody", Map.of(
                "contentType", "application/json",
                "fields", Map.of(
                        "id", "String (required) - Unique message identifier",
                        "key", "String (required) - Message partition key or device ID",
                        "payload", "String (required) - Raw payload content or JSON string",
                        "timestamp", "String (required) - ISO-8601 UTC creation timestamp"
                )
        ));
        persistEndpoint.put("responses", Map.of(
                "201", "Created: Message persisted successfully (first receipt)",
                "208", "Already Reported: Duplicate message ID detected (idempotent skip)",
                "400", "Bad Request: Missing or invalid required fields"
        ));

        Map<String, Object> healthEndpoint = new LinkedHashMap<>();
        healthEndpoint.put("path", "/actuator/health");
        healthEndpoint.put("method", "GET");
        healthEndpoint.put("summary", "Health and database connectivity check");
        healthEndpoint.put("responses", Map.of("200", "Service and PostgreSQL datasource UP"));

        Map<String, Object> apiDocsEndpoint = new LinkedHashMap<>();
        apiDocsEndpoint.put("path", "/api-docs");
        apiDocsEndpoint.put("method", "GET");
        apiDocsEndpoint.put("summary", "Live OpenAPI 3.0 JSON specification");

        Map<String, Object> swaggerEndpoint = new LinkedHashMap<>();
        swaggerEndpoint.put("path", "/swagger-ui.html");
        swaggerEndpoint.put("method", "GET");
        swaggerEndpoint.put("summary", "Interactive Swagger UI");

        Map<String, Object> sseEndpoint = new LinkedHashMap<>();
        sseEndpoint.put("path", "/sse");
        sseEndpoint.put("method", "GET");
        sseEndpoint.put("summary", "Spring AI Model Context Protocol (MCP) Server-Sent Events stream");

        docs.put("endpoints", List.of(
                persistEndpoint,
                healthEndpoint,
                apiDocsEndpoint,
                swaggerEndpoint,
                sseEndpoint
        ));

        return docs;
    }

    /**
     * MCP Tool: Check Persister Health and Database count as structured JSON.
     */
    @Tool(description = "Checks the operational health of the persister microservice and returns the current count of persisted messages in PostgreSQL.")
    public Map<String, Object> checkPersisterHealth() {
        log.info("[MCP Tool] Executing checkPersisterHealth");

        Map<String, Object> health = new LinkedHashMap<>();
        try {
            long count = repository.count();
            health.put("status", "UP");
            health.put("database", "Connected");
            health.put("totalProcessedMessages", count);
        } catch (Exception e) {
            health.put("status", "DEGRADED");
            health.put("database", "Disconnected");
            health.put("error", e.getMessage() != null ? e.getMessage() : "Unknown error");
        }

        return health;
    }
}
