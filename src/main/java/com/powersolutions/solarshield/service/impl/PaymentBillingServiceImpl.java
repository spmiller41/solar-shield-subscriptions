package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.entity.Invoice;
import com.powersolutions.solarshield.entity.PaymentBuffer;
import com.powersolutions.solarshield.enums.SquareBillingStatus;
import com.powersolutions.solarshield.repo.InvoiceRepo;
import com.powersolutions.solarshield.repo.PendingPaymentRepo;
import com.powersolutions.solarshield.service.api.PaymentBillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Applies Square payment events to invoices or buffers them until the invoice exists.
 */
@Service
public class PaymentBillingServiceImpl implements PaymentBillingService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentBillingServiceImpl.class);

    private final PendingPaymentRepo pendingPaymentRepo;
    private final InvoiceRepo invoiceRepo;

    public PaymentBillingServiceImpl(PendingPaymentRepo pendingPaymentRepo, InvoiceRepo invoiceRepo) {
        this.pendingPaymentRepo = pendingPaymentRepo;
        this.invoiceRepo = invoiceRepo;
    }

    /**
     * Routes the payment event to an existing invoice or buffers it for later replay.
     */
    @Override
    public Invoice processPaymentWebhook(SquareInvoicePaymentRequest request) {
        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            logger.warn("Skipping payment webhook eventId={} because orderId is missing. eventType={}",
                    request.getEventId(), request.getEventType());
            return null;
        }

        Optional<Invoice> optInvoice = invoiceRepo.findByOrderId(request.getOrderId());

        if (optInvoice.isPresent()) {
            logger.info("Applying payment webhook eventId={} to invoice orderId={} status={}",
                    request.getEventId(), request.getOrderId(), request.getStatus());
            return applyPaymentToInvoice(request, optInvoice.get());
        }

        logger.info("Buffering payment webhook eventId={} for unresolved orderId={} status={}",
                request.getEventId(), request.getOrderId(), request.getStatus());
        bufferPayment(request);
        return null;
    }

    /**
     * Stores the payment event until invoice processing can replay it.
     */
    private void bufferPayment(SquareInvoicePaymentRequest request) {
        pendingPaymentRepo.save(new PaymentBuffer(request));
        logger.info("Buffered payment webhook eventId={} for orderId={} status={}",
                request.getEventId(), request.getOrderId(), request.getStatus());
    }

    /**
     * Advances the invoice when the incoming payment status outranks the stored one.
     */
    private Invoice applyPaymentToInvoice(SquareInvoicePaymentRequest request, Invoice invoice) {
        String incomingStatus = request.getStatus();

        if (shouldAdvanceStatus(incomingStatus, invoice.getStatus())) {
            String previousStatus = invoice.getStatus();
            invoice.setStatus(incomingStatus);
            invoice.setUpdatedAt(LocalDateTime.now());
            Invoice savedInvoice = invoiceRepo.save(invoice);
            logger.info("Updated invoice orderId={} from status {} to {} via payment eventId={}",
                    invoice.getOrderId(), previousStatus, incomingStatus, request.getEventId());
            return savedInvoice;
        }

        logger.info("Ignored stale payment webhook eventId={} for orderId={}. currentStatus={}, incomingStatus={}",
                request.getEventId(), invoice.getOrderId(), invoice.getStatus(), incomingStatus);
        return invoice;
    }

    /**
     * Returns true when the incoming billing status outranks the current stored status.
     */
    private boolean shouldAdvanceStatus(String incomingStatus, String currentStatus) {
        SquareBillingStatus incoming = SquareBillingStatus.fromValue(incomingStatus);
        SquareBillingStatus current = SquareBillingStatus.fromValue(currentStatus);

        return incoming.getRank() > current.getRank();
    }

}
