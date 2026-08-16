package com.skylo.streaming.persister.service;

import com.skylo.streaming.persister.dto.MessageRequest;
import com.skylo.streaming.persister.dto.PersistStatus;

/**
 * Service interface defining message persistence and idempotency operations.
 */
public interface MessagePersistenceService {

    /**
     * Persists a message idempotently.
     *
     * @param request the message request payload
     * @return the persist status outcome (CREATED or DUPLICATE)
     */
    PersistStatus persist(MessageRequest request);
}
