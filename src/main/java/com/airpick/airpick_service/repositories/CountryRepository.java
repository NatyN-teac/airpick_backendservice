package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CountryRepository extends JpaRepository<Country, UUID> {

    List<Country> findAllByIsActiveTrueOrderByNameAsc();

    Optional<Country> findByNameIgnoreCase(String name);

    Optional<Country> findByCountryCodeIgnoreCase(String countryCode);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCountryCodeIgnoreCase(String countryCode);
}
