package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.entity.SquareWebhookEvent;
import com.powersolutions.solarshield.repo.SquareWebhookEventRepo;
import com.powersolutions.solarshield.service.api.SquareWebhookService;
import org.springframework.stereotype.Service;

@Service
public class SquareWebhookServiceImpl implements SquareWebhookService {

    private final SquareWebhookEventRepo eventRepo;

    public SquareWebhookServiceImpl(SquareWebhookEventRepo eventRepo) {
        this.eventRepo = eventRepo;
    }

    @Override
    public void handleWebhook(SquareInvoicePaymentRequest request) {

        if (eventRepo.existsByEventId(request.getEventId())) return;

        // route
        switch (request.getEventType()) {
            case INVOICE_CREATED:
            case INVOICE_UPDATED:
            case INVOICE_PAYMENT_MADE:
            case INVOICE_SCHEDULED_CHARGE_FAILED:
                // call invoiceBillingService
                break;

            case PAYMENT_CREATED:
            case PAYMENT_UPDATED:
                // call paymentBillingService
                break;
        }

        eventRepo.save(new SquareWebhookEvent(request));
    }

}
