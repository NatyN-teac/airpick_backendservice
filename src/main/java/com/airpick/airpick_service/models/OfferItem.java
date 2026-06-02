package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single item type the carrier is willing to carry as part of an {@link Offer}.
 * <p>
 * {@code quantity} is the total slots available for this item.
 * {@code remainingQuantity} decreases as {@link Match} requests come in and is
 * restored if a carrier cancels before pickup.
 * {@code pricePerItem} is what the carrier charges per unit (per piece, kg, or ml
 * depending on the item's {@link MeasurementUnit}).
 */
@Entity
@Table(name = "offer_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "remaining_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal remainingQuantity;

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
