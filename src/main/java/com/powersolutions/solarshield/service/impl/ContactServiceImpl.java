package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.repo.ContactRepo;
import com.powersolutions.solarshield.service.api.ContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Upserts contact records from intake data using phone first, then email, as lookup keys.
 */
@Service
public class ContactServiceImpl implements ContactService {

    private static final Logger logger = LoggerFactory.getLogger(ContactServiceImpl.class);

    private final ContactRepo contactRepo;

    public ContactServiceImpl(ContactRepo contactRepo) {
        this.contactRepo = contactRepo;
    }

    /**
     * Returns the existing contact for the incoming identity or creates a new one.
     */
    public Contact upsertAndGet(Contact incoming) {
        logger.info("Upserting contact using phone={} email={}", incoming.getPhone(), incoming.getEmail());

        return contactRepo.findByPhone(incoming.getPhone())
                .map(existing -> updateAndSave(existing, incoming))
                .or(() ->
                        contactRepo.findByEmail(incoming.getEmail())
                                .map(existing -> updateAndSave(existing, incoming))
                )
                .orElseGet(() -> createNew(incoming));
    }

    /**
     * Copies mutable intake fields onto an existing contact and persists the update.
     */
    private Contact updateAndSave(Contact existing, Contact incoming) {
        logger.info("Updating existing contact id={} with latest intake data", existing.getId());
        existing.setFirstName(incoming.getFirstName());
        existing.setLastName(incoming.getLastName());
        existing.setEmail(incoming.getEmail());
        existing.setPhone(incoming.getPhone());
        existing.setUpdatedAt(LocalDateTime.now());

        return contactRepo.save(existing);
    }

    /**
     * Initializes timestamps and persists a brand-new contact row.
     */
    private Contact createNew(Contact incoming) {
        incoming.setCreateAt(LocalDateTime.now());
        incoming.setUpdatedAt(LocalDateTime.now());

        Contact savedContact = contactRepo.save(incoming);
        logger.info("Created new contact id={} phone={} email={}",
                savedContact.getId(), savedContact.getPhone(), savedContact.getEmail());
        return savedContact;
    }

}
