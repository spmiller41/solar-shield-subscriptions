package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.entity.SquareWebhookEvent;
import com.powersolutions.solarshield.repo.SquareWebhookEventRepo;
import com.powersolutions.solarshield.service.api.SquareEventService;
import org.springframework.stereotype.Service;

@Service
public class SquareEventServiceImpl implements SquareEventService {

    private final SquareWebhookEventRepo eventRepo;

    public SquareEventServiceImpl(SquareWebhookEventRepo eventRepo) {
        this.eventRepo = eventRepo;
    }

    @Override
    public boolean hasProcessed(String eventId) {
        if (eventId == null) return false;
        return eventRepo.existsByEventId(eventId);
    }

    @Override
    public void recordProcessedEvent(SquareInvoicePaymentRequest request) {
        eventRepo.save(new SquareWebhookEvent(request));
    }

}
