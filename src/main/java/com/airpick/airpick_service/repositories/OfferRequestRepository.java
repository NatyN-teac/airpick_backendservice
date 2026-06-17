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

    @Query("""
            SELECT r FROM OfferRequest r
            WHERE r.status = 'OPEN'
              AND (CAST(:sourceCountry AS string) IS NULL
                   OR LOWER(r.sourceCountry) LIKE CONCAT('%', LOWER(CAST(:sourceCountry AS string)), '%'))
              AND (CAST(:destinationCountry AS string) IS NULL
                   OR LOWER(r.destinationCountry) LIKE CONCAT('%', LOWER(CAST(:destinationCountry AS string)), '%'))
            ORDER BY r.createdAt DESC
            """)
    List<OfferRequest> searchOpen(@Param("sourceCountry") String sourceCountry,
                                  @Param("destinationCountry") String destinationCountry);

    // Paging by status for admin
    org.springframework.data.domain.Page<OfferRequest> findByStatus(com.airpick.airpick_service.models.OfferRequestStatus status, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT r FROM OfferRequest r WHERE r.shipper.id = :shipperId AND r.status != 'CANCELLED' ORDER BY r.createdAt DESC")
    List<OfferRequest> findAllByShipperId(@Param("shipperId") UUID shipperId);

    // TODO: remove the own requests inclusion below when testing is done — revert to excluding shipper's own requests
    @Query("SELECT r FROM OfferRequest r WHERE r.status = 'OPEN' ORDER BY r.createdAt DESC")
    List<OfferRequest> findAllOpenExcludingShipper(@Param("shipperId") UUID shipperId);
}
