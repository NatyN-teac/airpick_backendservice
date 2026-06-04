package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.OfferProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferProposalRepository extends JpaRepository<OfferProposal, UUID> {

    @Query("SELECT p FROM OfferProposal p WHERE p.offerRequest.id = :requestId ORDER BY p.createdAt DESC")
    List<OfferProposal> findAllByOfferRequestId(@Param("requestId") UUID requestId);

    @Query("SELECT p FROM OfferProposal p WHERE p.carrier.id = :carrierId ORDER BY p.createdAt DESC")
    List<OfferProposal> findAllByCarrierId(@Param("carrierId") UUID carrierId);

    @Query("SELECT p FROM OfferProposal p WHERE p.offerRequest.id = :requestId AND p.status = 'PENDING' ORDER BY p.createdAt DESC")
    List<OfferProposal> findPendingByOfferRequestId(@Param("requestId") UUID requestId);

    /**
     * Returns the carrier's active (non-withdrawn, non-rejected) proposal for a given request.
     * Used to prevent duplicate proposals.
     */
    @Query("SELECT p FROM OfferProposal p WHERE p.offerRequest.id = :requestId AND p.carrier.id = :carrierId AND p.status NOT IN ('WITHDRAWN', 'REJECTED')")
    Optional<OfferProposal> findActiveByOfferRequestIdAndCarrierId(
            @Param("requestId") UUID requestId,
            @Param("carrierId") UUID carrierId);
}
