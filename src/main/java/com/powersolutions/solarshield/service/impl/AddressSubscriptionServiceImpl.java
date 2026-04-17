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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Resolves an intake address to a single subscription row and decides whether checkout can proceed.
 */
@Service
public class AddressSubscriptionServiceImpl implements AddressSubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(AddressSubscriptionServiceImpl.class);

    private final AddressRepo addressRepository;
    private final SubscriptionRepo subscriptionRepository;

    public AddressSubscriptionServiceImpl(AddressRepo addressRepository,
                                          SubscriptionRepo subscriptionRepository) {
        this.addressRepository = addressRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Finds or creates the address, then reuses or creates the subscription row tied to it.
     */
    public SubscriptionProcessingResult handleAddressAndSubscription(FormIntakeRequest request, Contact contact) {
        Address incomingAddress = new Address(request);
        normalizeAddress(incomingAddress);

        Address address = addressRepository.findByStreetAndCityAndZip(
                        incomingAddress.getStreet(),
                        incomingAddress.getCity(),
                        incomingAddress.getZip())
                .orElseGet(() -> {
                    Address savedAddress = addressRepository.save(incomingAddress);
                    logger.info("Created new address id={} street={} city={} zip={}",
                            savedAddress.getId(), savedAddress.getStreet(), savedAddress.getCity(), savedAddress.getZip());
                    return savedAddress;
                });

        Optional<Subscription> existingSubscription =
                subscriptionRepository.findByAddressId(address.getId());

        if (existingSubscription.isPresent()) {
            Subscription subscription = existingSubscription.get();

            if (subscription.getSubscriptionStatus() == SubscriptionStatus.ACTIVE) {
                logger.info("Blocked checkout because active subscriptionId={} already exists for addressId={}",
                        subscription.getId(), address.getId());
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
            logger.info("Reused subscriptionId={} for addressId={} and reset it to PENDING_PAYMENT",
                    subscription.getId(), address.getId());

            return new SubscriptionProcessingResult(subscription, SubscriptionResult.PROCEED_TO_CHECKOUT);
        }

        Subscription newSubscription =
                new Subscription(request, contact, address, SubscriptionStatus.PENDING_PAYMENT);

        subscriptionRepository.save(newSubscription);
        logger.info("Created new subscriptionId={} for addressId={} contactId={} with status={}",
                newSubscription.getId(), address.getId(), contact.getId(), newSubscription.getSubscriptionStatus());

        return new SubscriptionProcessingResult(newSubscription, SubscriptionResult.PROCEED_TO_CHECKOUT);
    }

    /**
     * Normalizes the address fields used for lookup so repeat submissions hit the same row.
     */
    private void normalizeAddress(Address address) {
        address.setStreet(normalize(address.getStreet()));
        address.setCity(normalize(address.getCity()));
        address.setZip(address.getZip() == null ? null : address.getZip().trim());
    }

    /**
     * Trims and uppercases a lookup field while preserving null values.
     */
    private String normalize(String value) { return value == null ? null : value.trim().toUpperCase(); }

}
