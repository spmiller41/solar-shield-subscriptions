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
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for processing Square invoice and payment webhook events.
 * <p>
 * Handles creation and updates of Invoice records in an idempotent, order-safe manner.
 * Supports out-of-order webhook delivery by creating invoices first and resolving
 * subscription linkage later when available.
 * <p>
 * Responsibilities:
 * - Resolve Subscription via order_id bridge when possible
 * - Upsert Invoice records with forward-only status progression
 * - Replay buffered payment updates once an invoice exists
 * <p>
 * Notes:
 * - order_id is the primary anchor for invoices
 * - subscription_id may be null initially and populated later
 * - Safe against duplicate and unordered webhook events
 */
@Service
public class InvoiceBillingServiceImpl implements InvoiceBillingService {

    private final PendingPaymentRepo paymentBuffer;
    private final SubscriptionRepo subscriptionRepo;
    private final InvoiceRepo invoiceRepo;

    public InvoiceBillingServiceImpl(PendingPaymentRepo paymentBuffer, SubscriptionRepo subscriptionRepo, InvoiceRepo invoiceRepo) {
        this.paymentBuffer = paymentBuffer;
        this.subscriptionRepo = subscriptionRepo;
        this.invoiceRepo = invoiceRepo;
    }

    /**
     * Orchestrates processing of an incoming Square invoice webhook.
     * <p>
     * Flow:
     * 1. Attempt to resolve the associated Subscription using the order_id bridge.
     * 2. Upsert the Invoice record:
     *    - Create a new record if it does not exist
     *    - Update existing record only if status advances
     *    - Attach subscription_id if available
     * 3. If order_id is present, replay any buffered payment events for this invoice.
     * <p>
     * Notes:
     * - Invoice records are created regardless of subscription availability to support
     *   out-of-order webhook delivery.
     * - Subscription linkage may be completed later when subscription.created arrives.
     * - Buffered payments are processed only after an invoice record exists.
     * - Safe against duplicate and unordered webhook events.
     *
     * @param request parsed Square invoice webhook payload
     */
    @Override
    public void processInvoiceWebhook(SquareInvoicePaymentRequest request) {
        Subscription subscription = findSubscriptionByOrderId(request);
        Invoice invoice = upsertInvoice(request, subscription);

        if (request.getOrderId() == null) return;

        handleBufferedPayments(request.getOrderId(), invoice);
    }

    /**
     * Attempts to retrieve a Subscription using the order_id bridge.
     * Returns null if not found or not resolvable.
     */
    @Override
    public Subscription findSubscriptionByOrderId(SquareInvoicePaymentRequest request) {
        if (request.getOrderId() == null) return null;
        return subscriptionRepo.findBySquareOrderId(request.getOrderId()).orElse(null);
    }

    /**
     * Creates or updates an Invoice record based on incoming Square webhook data.
     * <p>
     * Flow:
     * 1. Attempt to find an existing Invoice by order_id.
     * 2. If not found, create a new Invoice and populate:
     *    - order_id
     *    - initial status
     *    - subscription_id (if available)
     * 3. If found, compare incoming status against current status using rank-based evaluation.
     * 4. Update the Invoice status only if the incoming status represents forward progression.
     * 5. Persist and return the Invoice.
     * <p>
     * Notes:
     * - order_id uniquely identifies an invoice cycle from Square.
     * - Uses rank-based status comparison to prevent regression from out-of-order webhook events.
     * - Safe against duplicate webhook delivery (idempotent behavior).
     * - Subscription linkage is optional and may be resolved later if not available at creation time.
     *
     * @param request parsed Square invoice webhook payload
     * @param subscription resolved Subscription (might be null)
     * @return persisted Invoice entity (new or updated)
     */
    @Override
    public Invoice upsertInvoice(SquareInvoicePaymentRequest request, Subscription subscription) {

        String orderId = request.getOrderId();
        String incomingStatus = request.getStatus();

        Invoice invoice = invoiceRepo.findByOrderId(orderId)
                .orElseGet(() -> {
                    Invoice newInvoice = new Invoice();
                    newInvoice.setOrderId(orderId);
                    newInvoice.setStatus(incomingStatus);

                    if (subscription != null) {
                        newInvoice.setSubscriptionId(subscription.getId());
                    }

                    return newInvoice;
                });

        if (invoice.getId() != 0) {
            if (shouldAdvanceStatus(incomingStatus, invoice.getStatus())) {
                invoice.setStatus(incomingStatus);
            }
        }

        return invoiceRepo.save(invoice);
    }

    /**
     * Replays any buffered payment events for the given order after an invoice has been created.
     * <p>
     * Flow:
     * 1. Retrieve all buffered payments for the order_id.
     * 2. Determine the highest-rank payment status from the buffered events.
     * 3. Compare the strongest buffered status against the current invoice status.
     * 4. Update the invoice only if the buffered status represents a forward progression.
     * 5. Delete all buffered payments for the order once processed.
     * <p>
     * Notes:
     * - Uses rank-based evaluation to avoid applying stale or out-of-order payment updates.
     * - Processes only the most authoritative buffered state rather than applying updates sequentially.
     * - Safe against duplicate or unordered webhook delivery.
     */
    @Override
    public void handleBufferedPayments(String orderId, Invoice invoice) {
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
            invoice.setStatus(highestRankPayment.getStatus());
            invoiceRepo.save(invoice);
        }

        paymentBuffer.deleteByOrderId(orderId);
    }

    /**
     * Determines whether an incoming billing status should replace the current status.
     * <p>
     * Uses rank-based comparison to enforce forward-only state transitions:
     * - Higher rank -> update allowed
     * - Lower or equal rank -> ignored
     * <p>
     * Notes:
     * - Safely handles unknown or null values by mapping them to UNKNOWN (-1).
     * - Prevents regression from successful states (e.g., PAID -> FAILED).
     */
    @Override
    public boolean shouldAdvanceStatus(String incomingStatus, String currentStatus) {
        SquareBillingStatus incoming = SquareBillingStatus.fromValue(incomingStatus);
        SquareBillingStatus current = SquareBillingStatus.fromValue(currentStatus);

        return incoming.getRank() > current.getRank();
    }

}
