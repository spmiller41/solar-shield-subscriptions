package com.powersolutions.solarshield.service.impl;

import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.repo.ContactRepo;
import com.powersolutions.solarshield.service.api.ContactService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ContactServiceImpl implements ContactService {

    private final ContactRepo contactRepo;

    public ContactServiceImpl(ContactRepo contactRepo) {
        this.contactRepo = contactRepo;
    }

    /**
     * Upserts a Contact based on incoming data.
     * <p>
     * Flow:
     * 1. Attempt to find an existing contact by phone (primary identifier).
     * 2. If not found, attempt lookup by email (secondary identifier).
     * 3. If a match is found, update mutable fields (name, email, phone) and persist.
     * 4. If no match exists, create a new contact record.
     * <p>
     * Notes:
     * - Phone match takes precedence over email.
     * - Incoming data is treated as the latest source of truth for mutable fields.
     * - Ensures a single, up-to-date contact record per unique identity.
     *
     * @param incoming normalized contact data from form intake
     * @return persisted Contact (existing updated or newly created)
     */
    public Contact upsertAndGet(Contact incoming) {

        return contactRepo.findByPhone(incoming.getPhone())
                .map(existing -> updateAndSave(existing, incoming))
                .or(() ->
                        contactRepo.findByEmail(incoming.getEmail())
                                .map(existing -> updateAndSave(existing, incoming))
                )
                .orElseGet(() -> createNew(incoming));
    }

    private Contact updateAndSave(Contact existing, Contact incoming) {
        existing.setFirstName(incoming.getFirstName());
        existing.setLastName(incoming.getLastName());
        existing.setEmail(incoming.getEmail());
        existing.setPhone(incoming.getPhone());
        existing.setUpdatedAt(LocalDateTime.now());

        return contactRepo.save(existing);
    }

    private Contact createNew(Contact incoming) {
        incoming.setCreateAt(LocalDateTime.now());
        incoming.setUpdatedAt(LocalDateTime.now());

        return contactRepo.save(incoming);
    }

}
