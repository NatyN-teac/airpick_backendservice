package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.FlightLeg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FlightLegRepository extends JpaRepository<FlightLeg, UUID> {

    List<FlightLeg> findByFlightIdOrderByLegOrderAsc(UUID flightId);
}
