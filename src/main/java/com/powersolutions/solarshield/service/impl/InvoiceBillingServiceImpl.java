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

    @Override
    public void processInvoiceWebhook(SquareInvoicePaymentRequest request) {

    }

    @Override
    public Subscription ensureSubscriptionExists(SquareInvoicePaymentRequest request) {
        return null;
    }

    @Override
    public Invoice upsertInvoice(SquareInvoicePaymentRequest request, Subscription subscription) {
        return null;
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
