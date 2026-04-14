package com.powersolutions.solarshield.repo;

import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepo extends JpaRepository<Subscription, Integer> {

    Optional<Subscription> findByAddressId(int addressId);

    Optional<Subscription> findBySquareOrderId(String squareOrderId);

    Optional<Subscription> findByAddressIdAndSubscriptionStatus(int addressId, SubscriptionStatus subscriptionStatus);

}