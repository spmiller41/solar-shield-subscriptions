package com.powersolutions.solarshield.service.api;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.entity.Invoice;

public interface PaymentBillingService {

    Invoice processPaymentWebhook(SquareInvoicePaymentRequest request);

    void bufferPayment(SquareInvoicePaymentRequest request);

    Invoice applyPaymentToInvoice(SquareInvoicePaymentRequest request, Invoice invoice);

    boolean shouldAdvanceStatus(String incomingStatus, String currentStatus);

}
