package com.powersolutions.solarshield.repo;

import com.powersolutions.solarshield.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContactRepo extends JpaRepository<Contact, Integer> {

    Optional<Contact> findByPhone(String phone);

    Optional<Contact> findByEmail(String email);

}
