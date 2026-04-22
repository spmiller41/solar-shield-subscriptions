package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.dto.SquareCheckoutResponse;
import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.dto.SubscriptionProcessingResult;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.enums.SquareBillingStatus;
import com.powersolutions.solarshield.enums.SubscriptionResult;
import com.powersolutions.solarshield.enums.SubscriptionStatus;
import com.powersolutions.solarshield.repo.InvoiceRepo;
import com.powersolutions.solarshield.repo.SubscriptionRepo;
import com.powersolutions.solarshield.service.api.SubscriptionLifecycleService;
import com.powersolutions.solarshield.service.model.SubscriptionActivationResult;
import com.powersolutions.solarshield.service.square.SquareSubscriptionCheckoutService;
import com.powersolutions.solarshield.zoho.event.SubscriptionActivatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * Manages checkout-link creation and subscription activation as the Square lifecycle progresses.
 */
@Service
public class SubscriptionLifecycleServiceImpl implements SubscriptionLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionLifecycleServiceImpl.class);
    private static final Set<String> SUCCESSFUL_BILLING_STATUSES = Set.of(
            SquareBillingStatus.COMPLETED.name(),
            SquareBillingStatus.PAID.name()
    );
    private static final SquareCheckoutResponse PREBUILT_SQUARE_CHECKOUT_RESPONSE =
            new SquareCheckoutResponse(
                    "https://square.link/u/OZ2t040e",
                    "PvB1KnWu1kvUHjrNgpdGPoVDI3UZY",
                    null
            );

    private final SquareSubscriptionCheckoutService checkoutService;
    private final SubscriptionRepo subscriptionRepo;
    private final InvoiceRepo invoiceRepo;
    private final ApplicationEventPublisher eventPublisher;

    public SubscriptionLifecycleServiceImpl(SquareSubscriptionCheckoutService checkoutService,
                                            SubscriptionRepo subscriptionRepo,
                                            InvoiceRepo invoiceRepo,
                                            ApplicationEventPublisher eventPublisher) {
        this.checkoutService = checkoutService;
        this.subscriptionRepo = subscriptionRepo;
        this.invoiceRepo = invoiceRepo;
        this.eventPublisher = eventPublisher;
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
     * Links Square subscription identifiers to the local subscription without activating it.
     */
    @Override
    public Subscription linkSubscriptionFromWebhook(SquareInvoicePaymentRequest request) {
        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            logger.warn("Cannot link subscription from Square webhook eventId={} because orderId is missing",
                    request.getEventId());
            return null;
        }

        return subscriptionRepo.findBySquareOrderId(request.getOrderId())
                .map(sub -> {
                    sub.setCustomerSubscriptionId(request.getSubscriptionId());
                    sub.setCustomerId(request.getCustomerId());
                    sub.setEmail(request.getEmail());
                    sub.setUpdatedAt(LocalDateTime.now());

                    Subscription savedSub = subscriptionRepo.save(sub);

                    repairInvoicesForSubscription(savedSub);

                    if (hasSuccessfulLinkedInvoice(savedSub)) {
                        return activateSubscription(savedSub, request, "linked-paid-invoice").subscription();
                    }

                    logger.info("Linked subscriptionId={} from Square webhook eventId={} orderId={} customerSubscriptionId={}",
                            savedSub.getId(), request.getEventId(), request.getOrderId(), savedSub.getCustomerSubscriptionId());
                    return savedSub;
                })
                .orElseGet(() -> {
                    logger.warn("No subscription found for Square webhook eventId={} orderId={} during linking",
                            request.getEventId(), request.getOrderId());
                    return null;
                });
    }

    /**
     * Activates the local subscription after a confirmed Square billing success event.
     */
    @Override
    public SubscriptionActivationResult activateSubscriptionFromBillingWebhook(SquareInvoicePaymentRequest request) {
        if (isBlank(request.getSubscriptionId())) {
            logger.warn("Cannot activate subscription from billing webhook eventId={} because customerSubscriptionId is missing",
                    request.getEventId());
            return new SubscriptionActivationResult(null, false);
        }

        return subscriptionRepo.findByCustomerSubscriptionId(request.getSubscriptionId())
                .map(sub -> activateSubscription(sub, request, "billing-webhook"))
                .orElseGet(() -> {
                    logger.warn("No subscription found for billing webhook eventId={} customerSubscriptionId={} orderId={} during activation",
                            request.getEventId(), request.getSubscriptionId(), request.getOrderId());
                    return new SubscriptionActivationResult(null, false);
                });
    }

    /**
     * Returns true when the subscription already has a reusable checkout link.
     */
    private boolean hasCheckoutLink(Subscription sub) {
        return sub.getSquareCheckoutLink() != null && !sub.getSquareCheckoutLink().isBlank();
    }

    private void repairInvoicesForSubscription(Subscription subscription) {
        if (subscription.getCustomerSubscriptionId() == null || subscription.getCustomerSubscriptionId().isBlank()) {
            return;
        }

        invoiceRepo.repairInvoicesByCustomerSubscriptionId(
                subscription.getId(),
                subscription.getCustomerSubscriptionId()
        );
    }

    private boolean hasSuccessfulLinkedInvoice(Subscription subscription) {
        if (subscription.getCustomerSubscriptionId() == null || subscription.getCustomerSubscriptionId().isBlank()) {
            return false;
        }

        return invoiceRepo.existsByCustomerSubscriptionIdAndStatusIn(
                subscription.getCustomerSubscriptionId(),
                SUCCESSFUL_BILLING_STATUSES
        );
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

    private SubscriptionActivationResult activateSubscription(Subscription sub, SquareInvoicePaymentRequest request, String source) {
        boolean wasActive = sub.getSubscriptionStatus() == SubscriptionStatus.ACTIVE;

        sub.setCustomerSubscriptionId(firstNonBlank(request.getSubscriptionId(), sub.getCustomerSubscriptionId()));
        sub.setCustomerId(firstNonBlank(request.getCustomerId(), sub.getCustomerId()));
        sub.setEmail(firstNonBlank(request.getEmail(), sub.getEmail()));
        sub.setSubscriptionStatus(SubscriptionStatus.ACTIVE);

        if (sub.getActivatedAt() == null) {
            sub.setActivatedAt(LocalDateTime.now());
        }

        sub.setUpdatedAt(LocalDateTime.now());

        Subscription savedSub = subscriptionRepo.save(sub);
        repairInvoicesForSubscription(savedSub);

        if (!wasActive) {
            eventPublisher.publishEvent(new SubscriptionActivatedEvent(savedSub.getId()));
            logger.info("Activated subscriptionId={} from {} eventId={} orderId={} subscriptionStatus={}",
                    savedSub.getId(), source, request.getEventId(), request.getOrderId(), savedSub.getSubscriptionStatus());
            return new SubscriptionActivationResult(savedSub, true);
        }

        logger.info("SubscriptionId={} already ACTIVE; refreshed billing linkage from {} eventId={} orderId={}",
                savedSub.getId(), source, request.getEventId(), request.getOrderId());
        return new SubscriptionActivationResult(savedSub, false);
    }

    private String firstNonBlank(String preferredValue, String fallbackValue) {
        return preferredValue != null && !preferredValue.isBlank() ? preferredValue : fallbackValue;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
