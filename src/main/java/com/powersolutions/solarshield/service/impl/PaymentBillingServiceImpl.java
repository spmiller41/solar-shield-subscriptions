package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.entity.Invoice;
import com.powersolutions.solarshield.entity.PaymentBuffer;
import com.powersolutions.solarshield.enums.SquareBillingStatus;
import com.powersolutions.solarshield.repo.InvoiceRepo;
import com.powersolutions.solarshield.repo.PendingPaymentRepo;
import com.powersolutions.solarshield.service.api.PaymentBillingService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service responsible for processing Square payment webhook events.
 * <p>
 * Handles applying payment updates to existing Invoice records or buffering
 * events when the corresponding invoice is not yet available.
 * <p>
 * Responsibilities:
 * - Route payment events to Invoice when possible
 * - Buffer out-of-order payment events for later reconciliation
 * - Enforce forward-only billing status transitions
 * <p>
 * Notes:
 * - order_id is used to locate the target Invoice
 * - Buffered payments are replayed once the Invoice is created
 * - Safe against duplicate and unordered webhook delivery
 */
@Service
public class PaymentBillingServiceImpl implements PaymentBillingService {

    private final PendingPaymentRepo pendingPaymentRepo;
    private final InvoiceRepo invoiceRepo;

    public PaymentBillingServiceImpl(PendingPaymentRepo pendingPaymentRepo, InvoiceRepo invoiceRepo) {
        this.pendingPaymentRepo = pendingPaymentRepo;
        this.invoiceRepo = invoiceRepo;
    }

    /**
     * Processes an incoming payment webhook event.
     * <p>
     * Flow:
     * 1. Attempt to locate the Invoice by order_id.
     * 2. If found, apply the payment update to the Invoice.
     * 3. If not found, buffer the payment for later processing.
     *
     * @param request parsed Square webhook payload
     */
    @Override
    public void processPaymentWebhook(SquareInvoicePaymentRequest request) {
        Optional<Invoice> optInvoice = invoiceRepo.findByOrderId(request.getOrderId());

        if (optInvoice.isPresent()) {
            applyPaymentToInvoice(request, optInvoice.get());
            return;
        }

        bufferPayment(request);
    }

    /**
     * Buffers a payment event for later processing when the Invoice is not yet available.
     *
     * @param request parsed Square webhook payload
     */
    @Override
    public void bufferPayment(SquareInvoicePaymentRequest request) {
        pendingPaymentRepo.save(new PaymentBuffer(request));
    }

    /**
     * Applies a payment update to an existing Invoice if the incoming status represents
     * forward progression.
     * <p>
     * Updates the Invoice status and timestamp only if the new status has a higher rank.
     *
     * @param request parsed Square webhook payload
     * @param invoice target Invoice to update
     */
    @Override
    public void applyPaymentToInvoice(SquareInvoicePaymentRequest request, Invoice invoice) {
        String incomingStatus = request.getStatus();

        if (shouldAdvanceStatus(incomingStatus, invoice.getStatus())) {
            invoice.setStatus(incomingStatus);
            invoice.setUpdatedAt(LocalDateTime.now());
            invoiceRepo.save(invoice);
        }
    }

    /**
     * Determines whether an incoming billing status should replace the current status.
     * <p>
     * Uses rank-based comparison to enforce forward-only state transitions.
     *
     * @param incomingStatus new status from webhook
     * @param currentStatus current status stored on the Invoice
     * @return true if the incoming status represents forward progression
     */
    @Override
    public boolean shouldAdvanceStatus(String incomingStatus, String currentStatus) {
        SquareBillingStatus incoming = SquareBillingStatus.fromValue(incomingStatus);
        SquareBillingStatus current = SquareBillingStatus.fromValue(currentStatus);

        return incoming.getRank() > current.getRank();
    }

}
