package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlightRepository extends JpaRepository<Flight, UUID> {

    @Query("SELECT f FROM Flight f WHERE f.id = :id AND f.isDeleted = false")
    Optional<Flight> findActiveById(@Param("id") UUID id);

    @Query("SELECT f FROM Flight f WHERE f.isDeleted = false ORDER BY f.createdAt DESC")
    List<Flight> findAll();

    @Query("SELECT f FROM Flight f WHERE f.user.id = :userId AND f.isDeleted = false ORDER BY f.createdAt DESC")
    List<Flight> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT f FROM Flight f WHERE f.user.id = :userId AND f.isBooked = false AND f.isDeleted = false ORDER BY f.createdAt DESC")
    List<Flight> findAvailableByUserId(@Param("userId") UUID userId);
}
