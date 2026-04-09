package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.dto.FormIntakeRequest;
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
     * Handles address lookup and subscription intake logic using a single subscription row per address.
     * <p>
     * Flow:
     * 1. Normalize and find or create the address.
     * 2. Look up the subscription for that address.
     * 3. If the subscription is ACTIVE, block further processing.
     * 4. If the subscription exists but is not ACTIVE, update the same row as PENDING_PAYMENT
     *    with the latest contact and requested tier.
     * 5. If no subscription exists for the address, create a new PENDING_PAYMENT subscription.
     * <p>
     * Notes:
     * - Only one subscription record should exist per address.
     * - ACTIVE subscriptions block self-service checkout.
     * - Non-active subscriptions are reused as placeholders until payment is completed.
     * - Final plan tier is determined by the Square webhook, not the initial request.
     *
     * @param request incoming normalized form data
     * @param contact persisted contact associated with the request
     * @return SubscriptionResult indicating whether checkout should continue or an active subscription already exists
     */
    public SubscriptionResult handleAddressAndSubscription(FormIntakeRequest request, Contact contact) {
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
                return SubscriptionResult.ACTIVE_EXISTS;
            }

            subscription.setContactId(contact.getId());
            subscription.setPlanTier(request.getPlanTier());
            subscription.setSubscriptionStatus(SubscriptionStatus.PENDING_PAYMENT);
            subscription.setUpdatedAt(LocalDateTime.now());
            subscription.setActivatedAt(null);

            subscriptionRepository.save(subscription);

            return SubscriptionResult.PROCEED_TO_CHECKOUT;
        }

        Subscription newSubscription =
                new Subscription(request, contact, address, SubscriptionStatus.PENDING_PAYMENT);

        subscriptionRepository.save(newSubscription);

        return SubscriptionResult.PROCEED_TO_CHECKOUT;
    }

    private void normalizeAddress(Address address) {
        address.setStreet(normalize(address.getStreet()));
        address.setCity(normalize(address.getCity()));
        address.setZip(address.getZip() == null ? null : address.getZip().trim());
    }

    private String normalize(String value) { return value == null ? null : value.trim().toUpperCase(); }
}
