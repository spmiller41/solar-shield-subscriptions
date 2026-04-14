package com.powersolutions.solarshield.service.api;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.entity.Invoice;
import com.powersolutions.solarshield.entity.Subscription;

public interface InvoiceBillingService {

    void processInvoiceWebhook(SquareInvoicePaymentRequest request);

    Subscription findSubscriptionByOrderId(SquareInvoicePaymentRequest request);

    Invoice upsertInvoice(SquareInvoicePaymentRequest request, Subscription subscription);

    void handleBufferedPayments(String orderId, Invoice invoice);

    boolean shouldAdvanceStatus(String incomingStatus, String currentStatus);

}
