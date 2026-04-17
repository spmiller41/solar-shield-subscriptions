package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.entity.SquareWebhookEvent;
import com.powersolutions.solarshield.repo.SquareWebhookEventRepo;
import com.powersolutions.solarshield.service.api.SquareWebhookService;
import org.springframework.stereotype.Service;

@Service
public class SquareWebhookServiceImpl implements SquareWebhookService {

    private final SquareEventServiceImpl squareEventService;
    private final InvoiceBillingServiceImpl invoiceBillingService;
    private final PaymentBillingServiceImpl paymentBillingService;

    public SquareWebhookServiceImpl(SquareEventServiceImpl squareEventService,
                                    InvoiceBillingServiceImpl invoiceBillingService,
                                    PaymentBillingServiceImpl paymentBillingService) {
        this.squareEventService = squareEventService;
        this.invoiceBillingService = invoiceBillingService;
        this.paymentBillingService = paymentBillingService;
    }

    @Override
    public void handleWebhook(SquareInvoicePaymentRequest request) {

        if (squareEventService.hasProcessed(request.getEventId())) return;

        // route
        switch (request.getEventType()) {
            case SUBSCRIPTION_CREATED:
                // call SquareWebhookService
                break;

            case INVOICE_CREATED:
            case INVOICE_UPDATED:
            case INVOICE_PAYMENT_MADE:
            case INVOICE_SCHEDULED_CHARGE_FAILED:
                invoiceBillingService.processInvoiceWebhook(request);
                break;

            case PAYMENT_CREATED:
            case PAYMENT_UPDATED:
                paymentBillingService.processPaymentWebhook(request);
                break;
        }

        squareEventService.recordProcessedEvent(request);
    }

}
