package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Per-item price and quantity in a carrier's {@link OfferProposal}.
 * <p>
 * When the proposal is accepted, these values seed the {@link OfferItem}s
 * and {@link MatchedItem}s created from it.
 */
@Entity
@Table(name = "offer_proposal_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferProposalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private OfferProposal proposal;

    /**
     * The sender's item row this price applies to.
     * Item type and quantity are read from here — the carrier only adds pricePerItem.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_request_item_id", nullable = false)
    private OfferRequestItem offerRequestItem;

    @Column(name = "price_per_item", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerItem;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
