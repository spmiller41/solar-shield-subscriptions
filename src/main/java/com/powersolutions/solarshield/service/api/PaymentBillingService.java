package com.powersolutions.solarshield.service.api;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.service.model.InvoiceMutationResult;

public interface PaymentBillingService {

    InvoiceMutationResult processPaymentWebhook(SquareInvoicePaymentRequest request);

}
