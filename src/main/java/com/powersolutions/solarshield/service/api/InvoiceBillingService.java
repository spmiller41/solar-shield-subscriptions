package com.powersolutions.solarshield.service.api;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.entity.Invoice;

public interface InvoiceBillingService {

    Invoice processInvoiceWebhook(SquareInvoicePaymentRequest request);

}
