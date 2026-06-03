package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.Offer;
import com.airpick.airpick_service.models.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfferRepository extends JpaRepository<Offer, UUID> {

    @Query("SELECT o FROM Offer o WHERE o.id = :id AND o.status != 'CANCELLED'")
    Optional<Offer> findActiveById(@Param("id") UUID id);

    @Query("SELECT o FROM Offer o WHERE o.carrier.id = :carrierId ORDER BY o.createdAt DESC")
    List<Offer> findAllByCarrierId(@Param("carrierId") UUID carrierId);

    @Query("SELECT o FROM Offer o WHERE o.status = 'OPEN' ORDER BY o.createdAt DESC")
    List<Offer> findAllOpen();

    @Query("SELECT o FROM Offer o WHERE o.status = 'OPEN' AND o.carrier.id != :carrierId ORDER BY o.createdAt DESC")
    List<Offer> findAllOpenExcludingCarrier(@Param("carrierId") UUID carrierId);

    @Query("SELECT o FROM Offer o WHERE o.status = :status ORDER BY o.createdAt DESC")
    List<Offer> findAllByStatus(@Param("status") OfferStatus status);

    @Query("SELECT o FROM Offer o WHERE o.status = :status AND o.carrier.id != :carrierId ORDER BY o.createdAt DESC")
    List<Offer> findAllByStatusExcludingCarrier(@Param("status") OfferStatus status, @Param("carrierId") UUID carrierId);

    @Query("SELECT o FROM Offer o WHERE o.flight.id = :flightId AND o.offerSource = 'DIRECT' AND o.status != 'CANCELLED'")
    Optional<Offer> findDirectOfferByFlightId(@Param("flightId") UUID flightId);
}
