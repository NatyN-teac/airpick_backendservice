package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.OfferRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OfferRequestItemRepository extends JpaRepository<OfferRequestItem, UUID> {

    List<OfferRequestItem> findAllByOfferRequestId(UUID offerRequestId);
}
