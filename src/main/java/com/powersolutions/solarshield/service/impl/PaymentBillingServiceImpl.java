package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.entity.Invoice;
import com.powersolutions.solarshield.service.api.PaymentBillingService;
import org.springframework.stereotype.Service;

@Service
public class PaymentBillingServiceImpl implements PaymentBillingService {
    @Override
    public void processPaymentWebhook(SquareInvoicePaymentRequest request) {

    }

    @Override
    public void bufferPayment(SquareInvoicePaymentRequest request) {

    }

    @Override
    public void applyPaymentToInvoice(SquareInvoicePaymentRequest request, Invoice invoice) {

    }

    @Override
    public boolean shouldAdvanceStatus(String incomingStatus, String currentStatus) {
        return false;
    }
}
