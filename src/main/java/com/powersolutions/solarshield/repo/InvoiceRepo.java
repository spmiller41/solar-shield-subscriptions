package com.powersolutions.solarshield.repo;

import com.powersolutions.solarshield.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepo extends JpaRepository<Invoice, Integer> {

    Optional<Invoice> findByOrderId(String orderId);

}
