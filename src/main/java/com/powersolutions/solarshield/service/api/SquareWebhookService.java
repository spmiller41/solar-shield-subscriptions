package com.powersolutions.solarshield.service.api;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;

public interface SquareWebhookService {

    void handleWebhook(SquareInvoicePaymentRequest request);

}