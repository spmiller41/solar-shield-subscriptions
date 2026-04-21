package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.dto.SquareCheckoutResponse;
import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.dto.SubscriptionProcessingResult;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.enums.SubscriptionResult;
import com.powersolutions.solarshield.enums.SubscriptionStatus;
import com.powersolutions.solarshield.repo.InvoiceRepo;
import com.powersolutions.solarshield.repo.SubscriptionRepo;
import com.powersolutions.solarshield.service.api.SubscriptionLifecycleService;
import com.powersolutions.solarshield.service.square.SquareSubscriptionCheckoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Manages checkout-link creation and subscription activation as the Square lifecycle progresses.
 */
@Service
public class SubscriptionLifecycleServiceImpl implements SubscriptionLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionLifecycleServiceImpl.class);
    private static final SquareCheckoutResponse PREBUILT_SQUARE_CHECKOUT_RESPONSE =
            new SquareCheckoutResponse(
                    "https://square.link/u/OZ2t040e",
                    "PvB1KnWu1kvUHjrNgpdGPoVDI3UZY",
                    null
            );

    private final SquareSubscriptionCheckoutService checkoutService;
    private final SubscriptionRepo subscriptionRepo;
    private final InvoiceRepo invoiceRepo;

    public SubscriptionLifecycleServiceImpl(SquareSubscriptionCheckoutService checkoutService,
                                            SubscriptionRepo subscriptionRepo, InvoiceRepo invoiceRepo) {
        this.checkoutService = checkoutService;
        this.subscriptionRepo = subscriptionRepo;
        this.invoiceRepo = invoiceRepo;
    }

    /**
     * Reuses an existing checkout link or creates and stores a new one when checkout should proceed.
     */
    @Override
    public Optional<String> getOrCreateCheckoutLink(SubscriptionProcessingResult wrapper, Contact contact) {
        if (shouldSkipCheckoutLinkCreation(wrapper, "checkout link creation")) {
            return Optional.empty();
        }

        Optional<String> existingCheckoutLink = getExistingCheckoutLink(wrapper.getSubscription());
        if (existingCheckoutLink.isPresent()) {
            return existingCheckoutLink;
        }

        Subscription sub = wrapper.getSubscription();
        SquareCheckoutResponse response = checkoutService.createSubscriptionPaymentLink(sub, contact);
        return persistCheckoutLink(sub, response, "square-api");
    }

    /**
     * Reuses an existing checkout link or stores a fixed Square response captured from a live test.
     */
    public Optional<String> getOrCreateCheckoutLinkWithPrebuiltResponse(SubscriptionProcessingResult wrapper) {
        if (shouldSkipCheckoutLinkCreation(wrapper, "prebuilt checkout link creation")) {
            return Optional.empty();
        }

        Optional<String> existingCheckoutLink = getExistingCheckoutLink(wrapper.getSubscription());
        if (existingCheckoutLink.isPresent()) {
            return existingCheckoutLink;
        }

        Subscription sub = wrapper.getSubscription();
        logger.info("Using prebuilt Square checkout response for subscriptionId={} orderId={}",
                sub.getId(), PREBUILT_SQUARE_CHECKOUT_RESPONSE.getOrderId());
        return persistCheckoutLink(sub, PREBUILT_SQUARE_CHECKOUT_RESPONSE, "prebuilt-square-response");
    }

    /**
     * Activates the subscription tied to the webhook order id and repairs related invoices.
     */
    @Override
    public Subscription finalizeSubscriptionFromWebhook(SquareInvoicePaymentRequest request) {
        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            logger.warn("Cannot finalize subscription from Square webhook eventId={} because orderId is missing",
                    request.getEventId());
            return null;
        }

        return subscriptionRepo.findBySquareOrderId(request.getOrderId())
                .map(sub -> {
                    sub.setCustomerSubscriptionId(request.getSubscriptionId());
                    sub.setCustomerId(request.getCustomerId());
                    sub.setEmail(request.getEmail());
                    sub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
                    sub.setActivatedAt(LocalDateTime.now());
                    sub.setUpdatedAt(LocalDateTime.now());

                    Subscription savedSub = subscriptionRepo.save(sub);

                    invoiceRepo.repairInvoicesByCustomerSubscriptionId(
                            savedSub.getId(),
                            savedSub.getCustomerSubscriptionId()
                    );

                    logger.info("Finalized subscriptionId={} from Square webhook eventId={} orderId={} subscriptionStatus={}",
                            savedSub.getId(), request.getEventId(), request.getOrderId(), savedSub.getSubscriptionStatus());
                    return savedSub;
                })
                .orElseGet(() -> {
                    logger.warn("No subscription found for Square webhook eventId={} orderId={} during finalization",
                            request.getEventId(), request.getOrderId());
                    return null;
                });
    }

    /**
     * Returns true when the subscription already has a reusable checkout link.
     */
    private boolean hasCheckoutLink(Subscription sub) {
        return sub.getSquareCheckoutLink() != null && !sub.getSquareCheckoutLink().isBlank();
    }

    private boolean shouldSkipCheckoutLinkCreation(SubscriptionProcessingResult wrapper, String actionLabel) {
        if (wrapper.getResult() != SubscriptionResult.PROCEED_TO_CHECKOUT) {
            logger.info("Skipping {} for subscriptionId={} because result={}",
                    actionLabel,
                    wrapper.getSubscription() != null ? wrapper.getSubscription().getId() : null,
                    wrapper.getResult());
            return true;
        }

        return false;
    }

    private Optional<String> getExistingCheckoutLink(Subscription sub) {
        if (!hasCheckoutLink(sub)) {
            return Optional.empty();
        }

        logger.info("Reusing existing checkout link for subscriptionId={} orderId={}",
                sub.getId(), sub.getSquareOrderId());
        return Optional.of(sub.getSquareCheckoutLink());
    }

    private Optional<String> persistCheckoutLink(Subscription sub, SquareCheckoutResponse response, String source) {
        if (response == null || response.getCheckoutLink() == null || response.getCheckoutLink().isBlank()) {
            logger.warn("{} returned no usable checkout link for subscriptionId={}", source, sub.getId());
            return Optional.empty();
        }

        if (response.getOrderId() == null || response.getOrderId().isBlank()) {
            logger.warn("{} returned no usable orderId for subscriptionId={}", source, sub.getId());
            return Optional.empty();
        }

        sub.setSquareOrderId(response.getOrderId());
        sub.setSquareCheckoutLink(response.getCheckoutLink());
        subscriptionRepo.save(sub);
        logger.info("Stored checkout link for subscriptionId={} orderId={} source={}",
                sub.getId(), response.getOrderId(), source);
        return Optional.of(response.getCheckoutLink());
    }

}
