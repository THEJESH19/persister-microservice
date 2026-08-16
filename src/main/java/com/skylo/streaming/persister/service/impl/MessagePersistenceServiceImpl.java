package com.skylo.streaming.persister.service.impl;

import com.skylo.streaming.persister.dto.MessageRequest;
import com.skylo.streaming.persister.dto.PersistStatus;
import com.skylo.streaming.persister.entity.ProcessedMessage;
import com.skylo.streaming.persister.repository.ProcessedMessageRepository;
import com.skylo.streaming.persister.service.MessagePersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagePersistenceServiceImpl implements MessagePersistenceService {

    private final ProcessedMessageRepository repository;

    @Override
    @Transactional
    public PersistStatus persist(MessageRequest request) {
        log.info("Processing message persistence for ID: {}", request.getId());

        // Check first to avoid unnecessary inserts/exceptions
        if (repository.existsById(request.getId())) {
            log.warn("Duplicate message detected: {}. Skipping insert.", request.getId());
            return PersistStatus.DUPLICATE;
        }

        ProcessedMessage message = ProcessedMessage.builder()
                .id(request.getId())
                .messageKey(request.getKey())
                .payload(request.getPayload())
                .messageTimestamp(request.getTimestamp())
                .processedAt(Instant.now())
                .build();

        try {
            repository.saveAndFlush(message);
            log.info("Successfully saved message: {} with key: {}", request.getId(), request.getKey());
            return PersistStatus.CREATED;
        } catch (DataIntegrityViolationException e) {
            // Handle race conditions where another thread inserted the record between existsById check and save
            log.warn("Constraint violation on duplicate insert for ID: {}. Treating as successful idempotent operation.", request.getId());
            return PersistStatus.DUPLICATE;
        }
    }
}
