package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.entity.Invoice;
import com.powersolutions.solarshield.enums.SquareBillingStatus;
import com.powersolutions.solarshield.service.api.InvoiceBillingService;
import com.powersolutions.solarshield.service.api.PaymentBillingService;
import com.powersolutions.solarshield.service.api.SquareEventService;
import com.powersolutions.solarshield.service.api.SquareWebhookService;
import com.powersolutions.solarshield.service.api.SubscriptionLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Routes validated Square webhook events to the correct billing or subscription handler.
 */
@Service
public class SquareWebhookServiceImpl implements SquareWebhookService {

    private static final Logger logger = LoggerFactory.getLogger(SquareWebhookServiceImpl.class);

    private final SquareEventService squareEventService;
    private final InvoiceBillingService invoiceBillingService;
    private final PaymentBillingService paymentBillingService;
    private final SubscriptionLifecycleService subscriptionLifecycleService;

    public SquareWebhookServiceImpl(SquareEventService squareEventService,
                                    InvoiceBillingService invoiceBillingService,
                                    PaymentBillingService paymentBillingService,
                                    SubscriptionLifecycleService subscriptionLifecycleService) {
        this.squareEventService = squareEventService;
        this.invoiceBillingService = invoiceBillingService;
        this.paymentBillingService = paymentBillingService;
        this.subscriptionLifecycleService = subscriptionLifecycleService;
    }

    /**
     * Validates, de-duplicates, and routes a single Square webhook event within one transaction.
     */
    @Override
    @Transactional
    public void handleWebhook(SquareInvoicePaymentRequest request) {
        validateWebhookRequest(request);
        String eventId = request.getEventId();

        if (squareEventService.hasProcessed(eventId)) {
            logger.info("Skipping duplicate Square webhook eventId={} eventType={} orderId={}",
                    eventId, request.getEventType(), request.getOrderId());
            return;
        }

        logger.info("Processing Square webhook eventId={} eventType={} orderId={}",
                eventId, request.getEventType(), request.getOrderId());

        switch (request.getEventType()) {
            case SUBSCRIPTION_CREATED:
                subscriptionLifecycleService.linkSubscriptionFromWebhook(request);
                break;

            case INVOICE_CREATED:
            case INVOICE_UPDATED:
            case INVOICE_PAYMENT_MADE:
            case INVOICE_SCHEDULED_CHARGE_FAILED:
                Invoice invoice = invoiceBillingService.processInvoiceWebhook(request);
                activateSubscriptionIfInvoiceIsSuccessful(request, invoice);
                break;

            case PAYMENT_CREATED:
            case PAYMENT_UPDATED:
                paymentBillingService.processPaymentWebhook(request);
                break;
        }

        squareEventService.recordProcessedEvent(request);
        logger.info("Completed Square webhook eventId={} eventType={} orderId={}",
                eventId, request.getEventType(), request.getOrderId());
    }

    /**
     * Ensures the router has the minimum fields required to process a webhook safely.
     */
    private void validateWebhookRequest(SquareInvoicePaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Square webhook request is required");
        }

        if (request.getEventId() == null || request.getEventId().isBlank()) {
            throw new IllegalArgumentException("Square webhook eventId is required");
        }

        if (request.getEventType() == null) {
            throw new IllegalArgumentException("Square webhook eventType is required");
        }
    }

    private void activateSubscriptionIfInvoiceIsSuccessful(SquareInvoicePaymentRequest request, Invoice invoice) {
        if (invoice == null || !isSuccessfulBillingStatus(invoice.getStatus())) {
            return;
        }

        if ((request.getSubscriptionId() == null || request.getSubscriptionId().isBlank())
                && invoice.getCustomerSubscriptionId() != null
                && !invoice.getCustomerSubscriptionId().isBlank()) {
            request.setSubscriptionId(invoice.getCustomerSubscriptionId());
        }

        subscriptionLifecycleService.activateSubscriptionFromBillingWebhook(request);
    }

    private boolean isSuccessfulBillingStatus(String status) {
        return SquareBillingStatus.fromValue(status).getRank() >= SquareBillingStatus.COMPLETED.getRank();
    }

}
