package com.powersolutions.solarshield.repo;

import com.powersolutions.solarshield.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddressRepo extends JpaRepository<Address, Integer> {

    Optional<Address> findByStreetAndCityAndZip(String street, String city, String zip);

}
