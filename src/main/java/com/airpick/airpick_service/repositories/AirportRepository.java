package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AirportRepository extends JpaRepository<Airport, UUID> {

    @Query("SELECT a FROM Airport a WHERE a.isActive = true ORDER BY a.name ASC")
    List<Airport> findAllActive();

    @Query("SELECT a FROM Airport a WHERE a.id = :id AND a.isActive = true")
    Optional<Airport> findActiveById(@Param("id") UUID id);

    @Query("SELECT a FROM Airport a WHERE a.code = :code AND a.isActive = true")
    Optional<Airport> findActiveByCode(@Param("code") String code);
}
