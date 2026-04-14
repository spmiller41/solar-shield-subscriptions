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
