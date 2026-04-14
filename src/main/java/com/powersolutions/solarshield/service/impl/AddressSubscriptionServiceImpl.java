package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.dto.FormIntakeRequest;
import com.powersolutions.solarshield.dto.SubscriptionProcessingResult;
import com.powersolutions.solarshield.entity.Address;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.enums.SubscriptionResult;
import com.powersolutions.solarshield.enums.SubscriptionStatus;
import com.powersolutions.solarshield.repo.AddressRepo;
import com.powersolutions.solarshield.repo.SubscriptionRepo;
import com.powersolutions.solarshield.service.api.AddressSubscriptionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AddressSubscriptionServiceImpl implements AddressSubscriptionService {

    private final AddressRepo addressRepository;
    private final SubscriptionRepo subscriptionRepository;

    public AddressSubscriptionServiceImpl(AddressRepo addressRepository,
                                          SubscriptionRepo subscriptionRepository) {
        this.addressRepository = addressRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Handles address lookup and subscription intake using a single subscription row per address.
     * <p>
     * Flow:
     * 1. Normalize and find or create the address.
     * 2. Look up the subscription for that address.
     * 3. If the subscription is ACTIVE, return the existing row and block checkout.
     * 4. If the subscription exists but is not ACTIVE, reuse the same row as PENDING_PAYMENT:
     *    - update contact and requested tier
     *    - reset activation state
     *    - clear any stale checkout link/order data (for INACTIVE reuse)
     * 5. If no subscription exists for the address, create a new PENDING_PAYMENT subscription.
     * 6. Return both the resolved Subscription and the resulting intake decision.
     * <p>
     * Notes:
     * - Only one subscription record should exist per address.
     * - ACTIVE subscriptions block self-service checkout.
     * - INACTIVE subscriptions are reactivated as PENDING_PAYMENT and receive a fresh checkout link.
     * - The returned Subscription provides the caller with the row needed for checkout-link generation.
     * - Final plan tier is determined by the Square webhook, not the initial request.
     *
     * @param request incoming normalized form data
     * @param contact persisted contact associated with the request
     * @return wrapper containing the resolved Subscription and whether checkout should proceed
     */
    public SubscriptionProcessingResult handleAddressAndSubscription(FormIntakeRequest request, Contact contact) {
        Address incomingAddress = new Address(request);
        normalizeAddress(incomingAddress);

        Address address = addressRepository.findByStreetAndCityAndZip(
                        incomingAddress.getStreet(),
                        incomingAddress.getCity(),
                        incomingAddress.getZip())
                .orElseGet(() -> addressRepository.save(incomingAddress));

        Optional<Subscription> existingSubscription =
                subscriptionRepository.findByAddressId(address.getId());

        if (existingSubscription.isPresent()) {
            Subscription subscription = existingSubscription.get();

            if (subscription.getSubscriptionStatus() == SubscriptionStatus.ACTIVE) {
                return new SubscriptionProcessingResult(subscription, SubscriptionResult.ACTIVE_EXISTS);
            }

            subscription.setContactId(contact.getId());
            subscription.setPlanTier(request.getPlanTier());
            subscription.setSubscriptionStatus(SubscriptionStatus.PENDING_PAYMENT);
            subscription.setUpdatedAt(LocalDateTime.now());
            subscription.setActivatedAt(null);

            // Clear stale checkout data for reused (INACTIVE) subscriptions
            subscription.setSquareCheckoutLink(null);
            subscription.setSquareOrderId(null);

            subscriptionRepository.save(subscription);

            return new SubscriptionProcessingResult(subscription, SubscriptionResult.PROCEED_TO_CHECKOUT);
        }

        Subscription newSubscription =
                new Subscription(request, contact, address, SubscriptionStatus.PENDING_PAYMENT);

        subscriptionRepository.save(newSubscription);

        return new SubscriptionProcessingResult(newSubscription, SubscriptionResult.PROCEED_TO_CHECKOUT);
    }

    private void normalizeAddress(Address address) {
        address.setStreet(normalize(address.getStreet()));
        address.setCity(normalize(address.getCity()));
        address.setZip(address.getZip() == null ? null : address.getZip().trim());
    }

    private String normalize(String value) { return value == null ? null : value.trim().toUpperCase(); }

}
