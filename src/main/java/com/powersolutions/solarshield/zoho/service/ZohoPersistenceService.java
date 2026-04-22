package com.powersolutions.solarshield.zoho.service;

import com.powersolutions.solarshield.repo.InvoiceRepo;
import com.powersolutions.solarshield.repo.SubscriptionRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists Zoho linkage identifiers in their own transactions after external Zoho calls complete.
 */
@Service
public class ZohoPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(ZohoPersistenceService.class);

    private final SubscriptionRepo subscriptionRepo;
    private final InvoiceRepo invoiceRepo;

    public ZohoPersistenceService(SubscriptionRepo subscriptionRepo, InvoiceRepo invoiceRepo) {
        this.subscriptionRepo = subscriptionRepo;
        this.invoiceRepo = invoiceRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistSubscriptionZohoRecordId(int subscriptionId, String zohoRecordId) {
        int updatedRows = subscriptionRepo.updateZohoRecordId(subscriptionId, zohoRecordId);
        if (updatedRows != 1) {
            throw new IllegalStateException("Failed to persist zohoRecordId for subscriptionId " + subscriptionId);
        }

        logger.info("Persisted zohoRecordId {} for subscriptionId {}", zohoRecordId, subscriptionId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean persistInvoiceZohoSubformId(int invoiceId, String zohoSubformId) {
        int updatedRows = invoiceRepo.updateZohoSubformId(invoiceId, zohoSubformId);
        return updatedRows == 1;
    }

}
