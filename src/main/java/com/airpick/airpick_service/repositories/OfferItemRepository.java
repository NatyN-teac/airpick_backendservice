package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.OfferItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfferItemRepository extends JpaRepository<OfferItem, UUID> {

    @Query("SELECT oi FROM OfferItem oi WHERE oi.offer.id = :offerId")
    List<OfferItem> findAllByOfferId(@Param("offerId") UUID offerId);

    @Query("SELECT oi FROM OfferItem oi WHERE oi.id = :id AND oi.offer.id = :offerId")
    Optional<OfferItem> findByIdAndOfferId(@Param("id") UUID id, @Param("offerId") UUID offerId);
}
