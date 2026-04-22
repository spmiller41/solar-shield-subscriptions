package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.entity.SquareWebhookEvent;
import com.powersolutions.solarshield.repo.SquareWebhookEventRepo;
import com.powersolutions.solarshield.service.api.SquareEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Tracks processed Square webhook event ids so duplicate deliveries can be skipped.
 */
@Service
public class SquareEventServiceImpl implements SquareEventService {

    private static final Logger logger = LoggerFactory.getLogger(SquareEventServiceImpl.class);

    private final SquareWebhookEventRepo eventRepo;

    public SquareEventServiceImpl(SquareWebhookEventRepo eventRepo) {
        this.eventRepo = eventRepo;
    }

    /**
     * Returns whether the webhook event id has already been recorded.
     */
    @Override
    public boolean hasProcessed(String eventId) {
        if (eventId == null) {
            logger.warn("Cannot check Square webhook event processing because eventId is null");
            return false;
        }

        return eventRepo.existsByEventId(eventId);
    }

    /**
     * Persists the webhook event id after successful processing.
     */
    @Override
    public void recordProcessedEvent(SquareInvoicePaymentRequest request) {
        eventRepo.save(new SquareWebhookEvent(request));
        logger.info("Recorded processed Square webhook eventId={} eventType={}",
                request.getEventId(), request.getEventType());
    }

}
