package com.powersolutions.solarshield.repo;

import com.powersolutions.solarshield.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepo extends JpaRepository<Invoice, Integer> {

    Optional<Invoice> findByOrderId(String orderId);

    List<Invoice> findAllBySubscriptionId(Integer subscriptionId);

    boolean existsByCustomerSubscriptionIdAndStatusIn(String customerSubscriptionId, Collection<String> statuses);

    @Transactional
    @Modifying
    @Query("""
    UPDATE Invoice i
       SET i.subscriptionId = :subscriptionId
     WHERE i.customerSubscriptionId = :customerSubscriptionId
       AND i.subscriptionId IS NULL
    """)
    int repairInvoicesByCustomerSubscriptionId(Integer subscriptionId, String customerSubscriptionId);

}
