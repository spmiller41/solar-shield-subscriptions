package com.powersolutions.solarshield.repo;

import com.powersolutions.solarshield.entity.PaymentBuffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PendingPaymentRepo extends JpaRepository<PaymentBuffer, Integer> {

    List<PaymentBuffer> findByOrderId(String orderId);

    void deleteByOrderId(String orderId);

}
