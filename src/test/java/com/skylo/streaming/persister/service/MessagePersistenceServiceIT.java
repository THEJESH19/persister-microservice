package com.skylo.streaming.persister.service;

import com.skylo.streaming.persister.dto.MessageRequest;
import com.skylo.streaming.persister.dto.PersistStatus;
import com.skylo.streaming.persister.entity.ProcessedMessage;
import com.skylo.streaming.persister.repository.ProcessedMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class MessagePersistenceServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("persister_db_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MessagePersistenceService service;

    @Autowired
    private ProcessedMessageRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testPersistNewMessage_Success() {
        // Arrange
        String messageId = UUID.randomUUID().toString();
        MessageRequest request = MessageRequest.builder()
                .id(messageId)
                .key("device-101")
                .payload("First transmission")
                .timestamp(Instant.now())
                .build();

        // Act
        PersistStatus status = service.persist(request);

        // Assert
        assertThat(status).isEqualTo(PersistStatus.CREATED);
        
        Optional<ProcessedMessage> persisted = repository.findById(messageId);
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getMessageKey()).isEqualTo("device-101");
        assertThat(persisted.get().getPayload()).isEqualTo("First transmission");
    }

    @Test
    void testPersistDuplicateMessage_IdempotentSkip() {
        // Arrange
        String messageId = UUID.randomUUID().toString();
        MessageRequest request = MessageRequest.builder()
                .id(messageId)
                .key("device-101")
                .payload("Repeated transmission")
                .timestamp(Instant.now())
                .build();

        // Act - First persistence
        PersistStatus firstStatus = service.persist(request);
        // Act - Second persistence (duplicate)
        PersistStatus secondStatus = service.persist(request);

        // Assert
        assertThat(firstStatus).isEqualTo(PersistStatus.CREATED);
        assertThat(secondStatus).isEqualTo(PersistStatus.DUPLICATE);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void testConcurrentPersistSameMessage_IdempotentHandling() throws Exception {
        // Arrange
        String messageId = UUID.randomUUID().toString();
        MessageRequest request = MessageRequest.builder()
                .id(messageId)
                .key("device-101")
                .payload("Concurrent transmission")
                .timestamp(Instant.now())
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Act
        Future<PersistStatus> future1 = executor.submit(() -> service.persist(request));
        Future<PersistStatus> future2 = executor.submit(() -> service.persist(request));

        PersistStatus status1 = future1.get();
        PersistStatus status2 = future2.get();

        executor.shutdown();

        // Assert
        // One thread must succeed (CREATED), and the other must handle conflict gracefully (DUPLICATE)
        assertThat(status1).isNotEqualTo(status2);
        assertThat(repository.count()).isEqualTo(1);
    }
}
