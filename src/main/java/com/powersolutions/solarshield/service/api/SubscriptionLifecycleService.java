package com.powersolutions.solarshield.service.api;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.dto.SubscriptionProcessingResult;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.service.model.SubscriptionActivationResult;

import java.util.Optional;

public interface SubscriptionLifecycleService {

    public Optional<String> getOrCreateCheckoutLink(SubscriptionProcessingResult wrapper, Contact contact);

    public Subscription linkSubscriptionFromWebhook(SquareInvoicePaymentRequest request);

    public SubscriptionActivationResult activateSubscriptionFromBillingWebhook(SquareInvoicePaymentRequest request);

}
