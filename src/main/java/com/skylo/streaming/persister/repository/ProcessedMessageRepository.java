package com.skylo.streaming.persister.repository;

import com.skylo.streaming.persister.entity.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for database operations on ProcessedMessage entities.
 */
@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {
}
