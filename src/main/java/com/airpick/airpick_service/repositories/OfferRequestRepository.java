package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.OfferRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OfferRequestRepository extends JpaRepository<OfferRequest, UUID> {

    @Query("SELECT r FROM OfferRequest r WHERE r.status = 'OPEN' ORDER BY r.createdAt DESC")
    List<OfferRequest> findAllOpen();

    @Query("SELECT r FROM OfferRequest r WHERE r.shipper.id = :shipperId ORDER BY r.createdAt DESC")
    List<OfferRequest> findAllByShipperId(@Param("shipperId") UUID shipperId);

    @Query("SELECT r FROM OfferRequest r WHERE r.status = 'OPEN' AND r.shipper.id != :shipperId ORDER BY r.createdAt DESC")
    List<OfferRequest> findAllOpenExcludingShipper(@Param("shipperId") UUID shipperId);
}
