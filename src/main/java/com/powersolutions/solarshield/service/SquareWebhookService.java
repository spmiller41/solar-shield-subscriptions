package com.powersolutions.solarshield.service;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;

public interface SquareWebhookService {

    void handleWebhook(SquareInvoicePaymentRequest request);

}