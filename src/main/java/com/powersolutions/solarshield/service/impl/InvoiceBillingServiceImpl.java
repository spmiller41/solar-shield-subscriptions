package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.entity.Invoice;
import com.powersolutions.solarshield.entity.PaymentBuffer;
import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.enums.SquareBillingStatus;
import com.powersolutions.solarshield.repo.InvoiceRepo;
import com.powersolutions.solarshield.repo.PendingPaymentRepo;
import com.powersolutions.solarshield.repo.SubscriptionRepo;
import com.powersolutions.solarshield.service.api.InvoiceBillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Applies Square invoice events to local invoices and replays buffered payment updates when needed.
 */
@Service
public class InvoiceBillingServiceImpl implements InvoiceBillingService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceBillingServiceImpl.class);

    private final PendingPaymentRepo paymentBuffer;
    private final SubscriptionRepo subscriptionRepo;
    private final InvoiceRepo invoiceRepo;

    public InvoiceBillingServiceImpl(PendingPaymentRepo paymentBuffer, SubscriptionRepo subscriptionRepo, InvoiceRepo invoiceRepo) {
        this.paymentBuffer = paymentBuffer;
        this.subscriptionRepo = subscriptionRepo;
        this.invoiceRepo = invoiceRepo;
    }

    /**
     * Resolves the subscription, upserts the invoice, and then replays any buffered payments.
     */
    @Override
    public Invoice processInvoiceWebhook(SquareInvoicePaymentRequest request) {
        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            logger.warn("Skipping invoice webhook eventId={} because orderId is missing. eventType={}",
                    request.getEventId(), request.getEventType());
            return null;
        }

        Subscription subscription = findSubscriptionByCustomerSubscriptionId(request);
        Invoice invoice = upsertInvoice(request, subscription);

        handleBufferedPayments(request.getOrderId(), invoice);
        return invoice;
    }

    /**
     * Resolves the subscription tied to the webhook customerSubscriptionId, if one exists.
     */
    private Subscription findSubscriptionByCustomerSubscriptionId(SquareInvoicePaymentRequest request) {
        if (isBlank(request.getSubscriptionId())) {
            logger.info("Invoice webhook eventId={} has no customerSubscriptionId yet; invoice will remain detached for now. orderId={}",
                    request.getEventId(), request.getOrderId());
            return null;
        }

        return subscriptionRepo.findByCustomerSubscriptionId(request.getSubscriptionId())
                .map(subscription -> {
                    logger.info("Resolved invoice webhook eventId={} by customerSubscriptionId={} orderId={}",
                            request.getEventId(), request.getSubscriptionId(), request.getOrderId());
                    return subscription;
                })
                .orElse(null);
    }

    /**
     * Creates the invoice if missing or advances its status when the incoming event is newer.
     */
    private Invoice upsertInvoice(SquareInvoicePaymentRequest request, Subscription subscription) {

        String orderId = request.getOrderId();
        String incomingStatus = request.getStatus();
        LocalDateTime now = LocalDateTime.now();

        Invoice invoice = invoiceRepo.findByOrderId(orderId)
                .orElseGet(() -> {
                    Invoice newInvoice = new Invoice();
                    newInvoice.setOrderId(orderId);
                    newInvoice.setCustomerSubscriptionId(request.getSubscriptionId());
                    newInvoice.setAmount(request.getAmount());
                    newInvoice.setCurrency(request.getCurrency());
                    newInvoice.setStatus(incomingStatus);
                    newInvoice.setUpdatedAt(now);

                    if (subscription != null) {
                        newInvoice.setSubscriptionId(subscription.getId());
                    }

                    return newInvoice;
                });

        if (invoice.getId() != 0) {
            if (request.getSubscriptionId() != null && !request.getSubscriptionId().isBlank()) {
                invoice.setCustomerSubscriptionId(request.getSubscriptionId());
            }

            if (request.getAmount() != null) {
                invoice.setAmount(request.getAmount());
            }

            if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
                invoice.setCurrency(request.getCurrency());
            }

            if (subscription != null && invoice.getSubscriptionId() == null) {
                invoice.setSubscriptionId(subscription.getId());
            }

            if (shouldAdvanceStatus(incomingStatus, invoice.getStatus())) {
                invoice.setStatus(incomingStatus);
            }

            invoice.setUpdatedAt(now);
        }

        return invoiceRepo.save(invoice);
    }

    /**
     * Applies the strongest buffered payment status for the order and clears the buffer.
     */
    private void handleBufferedPayments(String orderId, Invoice invoice) {
        List<PaymentBuffer> bufferList = paymentBuffer.findByOrderId(orderId);
        if (bufferList.isEmpty()) return;

        PaymentBuffer highestRankPayment = null;
        int highestRank = Integer.MIN_VALUE;

        for (PaymentBuffer buffer : bufferList) {
            SquareBillingStatus status = SquareBillingStatus.fromValue(buffer.getStatus());

            if (status.getRank() > highestRank) {
                highestRank = status.getRank();
                highestRankPayment = buffer;
            }
        }

        if (highestRankPayment != null &&
                shouldAdvanceStatus(highestRankPayment.getStatus(), invoice.getStatus())) {
            String previousStatus = invoice.getStatus();
            invoice.setStatus(highestRankPayment.getStatus());
            invoice.setUpdatedAt(LocalDateTime.now());
            invoiceRepo.save(invoice);
            logger.info("Promoted invoice orderId={} from status {} to {} using buffered payment status={}",
                    invoice.getOrderId(), previousStatus, highestRankPayment.getStatus(), highestRankPayment.getStatus());
        }

        paymentBuffer.deleteByOrderId(orderId);
    }

    /**
     * Returns true when the incoming billing status outranks the current stored status.
     */
    private boolean shouldAdvanceStatus(String incomingStatus, String currentStatus) {
        SquareBillingStatus incoming = SquareBillingStatus.fromValue(incomingStatus);
        SquareBillingStatus current = SquareBillingStatus.fromValue(currentStatus);

        return incoming.getRank() > current.getRank();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
