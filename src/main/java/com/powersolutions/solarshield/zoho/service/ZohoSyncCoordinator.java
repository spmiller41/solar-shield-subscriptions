package com.powersolutions.solarshield.zoho.service;

import com.powersolutions.solarshield.entity.Address;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.entity.Invoice;
import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.enums.SubscriptionStatus;
import com.powersolutions.solarshield.repo.AddressRepo;
import com.powersolutions.solarshield.repo.ContactRepo;
import com.powersolutions.solarshield.repo.InvoiceRepo;
import com.powersolutions.solarshield.repo.SubscriptionRepo;
import com.powersolutions.solarshield.zoho.event.InvoiceChangedEvent;
import com.powersolutions.solarshield.zoho.event.SubscriptionActivatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Coordinates Zoho sync after local subscription and invoice transactions commit.
 */
@Service
public class ZohoSyncCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(ZohoSyncCoordinator.class);

    private final ZohoAccountService zohoAccountService;
    private final SubscriptionRepo subscriptionRepo;
    private final ContactRepo contactRepo;
    private final AddressRepo addressRepo;
    private final InvoiceRepo invoiceRepo;

    public ZohoSyncCoordinator(ZohoAccountService zohoAccountService,
                               SubscriptionRepo subscriptionRepo,
                               ContactRepo contactRepo,
                               AddressRepo addressRepo,
                               InvoiceRepo invoiceRepo) {
        this.zohoAccountService = zohoAccountService;
        this.subscriptionRepo = subscriptionRepo;
        this.contactRepo = contactRepo;
        this.addressRepo = addressRepo;
        this.invoiceRepo = invoiceRepo;
    }

    /**
     * Syncs a single invoice row only when the parent Zoho record is already linked locally.
     */
    @Order(2)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInvoiceChanged(InvoiceChangedEvent event) {
        if (event.activationSyncScheduled()) {
            return;
        }

        Invoice invoice = invoiceRepo.findById(event.invoiceId())
                .orElse(null);

        if (invoice == null) {
            logger.warn("Skipping Zoho invoice sync because invoiceId={} no longer exists", event.invoiceId());
            return;
        }

        if (invoice.getSubscriptionId() == null) {
            logger.info("Skipping Zoho invoice sync for invoiceId={} because it is not linked to a subscription yet",
                    invoice.getId());
            return;
        }

        Subscription subscription = subscriptionRepo.findById(invoice.getSubscriptionId())
                .orElse(null);

        if (subscription == null) {
            logger.warn("Skipping Zoho invoice sync for invoiceId={} because subscriptionId={} no longer exists",
                    invoice.getId(), invoice.getSubscriptionId());
            return;
        }

        if (subscription.getZohoRecordId() == null || subscription.getZohoRecordId().isBlank()) {
            logger.info("Skipping Zoho invoice sync for invoiceId={} because subscriptionId={} has no zohoRecordId yet",
                    invoice.getId(), subscription.getId());
            return;
        }

        zohoAccountService.syncAccountInvoices(subscription, List.of(invoice));
    }

    /**
     * Upserts the parent Zoho account and syncs all linked invoices when a subscription first becomes ACTIVE.
     */
    @Order(1)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSubscriptionActivated(SubscriptionActivatedEvent event) {
        Subscription subscription = subscriptionRepo.findById(event.subscriptionId())
                .orElse(null);

        if (subscription == null) {
            logger.warn("Skipping Zoho activation sync because subscriptionId={} no longer exists", event.subscriptionId());
            return;
        }

        if (subscription.getSubscriptionStatus() != SubscriptionStatus.ACTIVE) {
            logger.info("Skipping Zoho activation sync for subscriptionId={} because status={}",
                    subscription.getId(), subscription.getSubscriptionStatus());
            return;
        }

        Contact contact = contactRepo.findById(subscription.getContactId())
                .orElseThrow(() -> new IllegalStateException(
                        "Unable to load contactId " + subscription.getContactId() + " for subscriptionId " + subscription.getId()
                ));

        Address address = addressRepo.findById(subscription.getAddressId())
                .orElseThrow(() -> new IllegalStateException(
                        "Unable to load addressId " + subscription.getAddressId() + " for subscriptionId " + subscription.getId()
                ));

        zohoAccountService.upsertAccount(subscription, address, contact);

        List<Invoice> invoices = invoiceRepo.findAllBySubscriptionId(subscription.getId());
        if (invoices.isEmpty()) {
            logger.info("Zoho activation sync found no invoices for subscriptionId={}", subscription.getId());
            return;
        }

        zohoAccountService.syncAccountInvoices(subscription, invoices);
    }

}
