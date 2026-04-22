package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.enums.SquareBillingStatus;
import com.powersolutions.solarshield.service.api.InvoiceBillingService;
import com.powersolutions.solarshield.service.api.PaymentBillingService;
import com.powersolutions.solarshield.service.api.SquareEventService;
import com.powersolutions.solarshield.service.api.SquareWebhookService;
import com.powersolutions.solarshield.service.api.SubscriptionLifecycleService;
import com.powersolutions.solarshield.service.model.InvoiceMutationResult;
import com.powersolutions.solarshield.zoho.event.InvoiceChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    public SquareWebhookServiceImpl(SquareEventService squareEventService,
                                    InvoiceBillingService invoiceBillingService,
                                    PaymentBillingService paymentBillingService,
                                    SubscriptionLifecycleService subscriptionLifecycleService,
                                    ApplicationEventPublisher eventPublisher) {
        this.squareEventService = squareEventService;
        this.invoiceBillingService = invoiceBillingService;
        this.paymentBillingService = paymentBillingService;
        this.subscriptionLifecycleService = subscriptionLifecycleService;
        this.eventPublisher = eventPublisher;
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
                InvoiceMutationResult invoiceResult = invoiceBillingService.processInvoiceWebhook(request);
                activateSubscriptionIfInvoiceIsSuccessful(request, invoiceResult);
                publishInvoiceChangedEvent(invoiceResult);
                break;

            case PAYMENT_CREATED:
            case PAYMENT_UPDATED:
                InvoiceMutationResult paymentResult = paymentBillingService.processPaymentWebhook(request);
                activateSubscriptionIfInvoiceIsSuccessful(request, paymentResult);
                publishInvoiceChangedEvent(paymentResult);
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

    private void activateSubscriptionIfInvoiceIsSuccessful(SquareInvoicePaymentRequest request, InvoiceMutationResult result) {
        if (result == null || !result.changed() || result.invoice() == null || !isSuccessfulBillingStatus(result.invoice().getStatus())) {
            return;
        }

        var invoice = result.invoice();
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

    private void publishInvoiceChangedEvent(InvoiceMutationResult result) {
        if (result == null || !result.changed() || result.invoice() == null || result.invoice().getId() <= 0) {
            return;
        }

        eventPublisher.publishEvent(new InvoiceChangedEvent(result.invoice().getId()));
    }

}
