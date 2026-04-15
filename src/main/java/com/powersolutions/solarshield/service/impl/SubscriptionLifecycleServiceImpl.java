package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.dto.SquareCheckoutResponse;
import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.dto.SubscriptionProcessingResult;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.enums.SubscriptionResult;
import com.powersolutions.solarshield.enums.SubscriptionStatus;
import com.powersolutions.solarshield.repo.SubscriptionRepo;
import com.powersolutions.solarshield.service.api.SubscriptionLifecycleService;
import com.powersolutions.solarshield.service.square.SquareSubscriptionCheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SubscriptionLifecycleServiceImpl implements SubscriptionLifecycleService {

    private final SquareSubscriptionCheckoutService checkoutService;
    private final SubscriptionRepo subscriptionRepo;

    @Autowired
    public SubscriptionLifecycleServiceImpl(SquareSubscriptionCheckoutService checkoutService,
                                            SubscriptionRepo subscriptionRepo) {
        this.checkoutService = checkoutService;
        this.subscriptionRepo = subscriptionRepo;
    }

    /**
     * Retrieves an existing checkout link for the given Subscription or creates one if eligible.
     * <p>
     * Flow:
     * 1. If checkout should not proceed, return empty.
     * 2. If a checkout link already exists, return it.
     * 3. Otherwise, generate a new checkout link via Square.
     * 4. Persist the returned order_id and checkout link to the Subscription.
     * 5. Return the checkout link for client redirection.
     * <p>
     * Notes:
     * - Prevents checkout for ACTIVE subscriptions.
     * - Ensures idempotent behavior by reusing existing links when available.
     * - Associates the Square order_id with the Subscription for webhook mapping.
     *
     * @param wrapper resolved Subscription and intake result
     * @param contact contact associated with the subscription
     * @return Optional containing the checkout link if available
     */
    @Override
    public Optional<String> getOrCreateCheckoutLink(SubscriptionProcessingResult wrapper, Contact contact) {
        if (wrapper.getResult() != SubscriptionResult.PROCEED_TO_CHECKOUT) return Optional.empty();

        Subscription sub = wrapper.getSubscription();
        if (hasCheckoutLink(sub)) return Optional.of(sub.getSquareCheckoutLink());

        SquareCheckoutResponse response = checkoutService.createSubscriptionPaymentLink(sub, contact);
        if (response == null || response.getCheckoutLink() == null || response.getCheckoutLink().isBlank()) {
            return Optional.empty();
        }

        sub.setSquareOrderId(response.getOrderId());
        sub.setSquareCheckoutLink(response.getCheckoutLink());
        subscriptionRepo.save(sub);

        return Optional.of(response.getCheckoutLink());
    }

    /**
     * Finalizes a Subscription from the Square webhook by resolving via order_id,
     * setting identifiers, and marking it ACTIVE.
     *
     * @param request parsed Square webhook payload
     * @return updated Subscription or null if not found
     */
    @Override
    public Subscription finalizeSubscriptionFromWebhook(SquareInvoicePaymentRequest request) {
        return subscriptionRepo.findBySquareOrderId(request.getOrderId())
                .map(sub -> {
                    sub.setCustomerSubscriptionId(request.getSubscriptionId());
                    sub.setCustomerId(request.getCustomerId());
                    sub.setEmail(request.getEmail());
                    sub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
                    sub.setActivatedAt(LocalDateTime.now());
                    sub.setUpdatedAt(LocalDateTime.now());
                    return subscriptionRepo.save(sub);
                })
                .orElse(null);
    }

    private boolean hasCheckoutLink(Subscription sub) {
        return sub.getSquareCheckoutLink() != null && !sub.getSquareCheckoutLink().isBlank();
    }

}
