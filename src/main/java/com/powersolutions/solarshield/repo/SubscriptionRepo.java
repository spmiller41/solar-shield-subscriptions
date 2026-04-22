package com.powersolutions.solarshield.repo;

import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface SubscriptionRepo extends JpaRepository<Subscription, Integer> {

    Optional<Subscription> findByAddressId(int addressId);

    Optional<Subscription> findBySquareOrderId(String squareOrderId);

    Optional<Subscription> findByCustomerSubscriptionId(String customerSubscriptionId);

    Optional<Subscription> findByAddressIdAndSubscriptionStatus(int addressId, SubscriptionStatus subscriptionStatus);

    @Transactional
    @Modifying
    @Query("""
    UPDATE Subscription s
       SET s.zohoRecordId = :zohoRecordId
     WHERE s.id = :subscriptionId
    """)
    int updateZohoRecordId(Integer subscriptionId, String zohoRecordId);

}
