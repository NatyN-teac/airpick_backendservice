package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An offer posted by a carrier advertising space on their flight.
 * <p>
 * {@code offerSource = DIRECT} means the carrier created it manually on the marketplace.
 * {@code offerSource = FROM_PROPOSAL} means it was generated when a sender accepted
 * a carrier's proposal on an {@link OfferRequest}; in this case {@code offerRequest} is set.
 * <p>
 * Slot availability is tracked per {@link OfferItem} via {@code remainingQuantity}.
 * The offer moves to {@link OfferStatus#FULLY_MATCHED} when all offer items are exhausted.
 */
@Entity
@Table(name = "offers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id", nullable = false)
    private User carrier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OfferStatus status = OfferStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_source", nullable = false, length = 20)
    @Builder.Default
    private OfferSource offerSource = OfferSource.DIRECT;

    /** Null when offerSource is DIRECT; points to the originating request when FROM_PROPOSAL. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_request_id")
    private OfferRequest offerRequest;

    /** ISO 4217 currency code for all item prices on this offer (e.g. USD, EUR, NGN). */
    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    /**
     * True when at least one offer item was manually submitted by a carrier and
     * is pending admin approval. The offer stays in PENDING_ITEM_APPROVAL until
     * all manual items are approved, then transitions to OPEN automatically.
     */
    @Column(name = "has_manual_item", nullable = false)
    @Builder.Default
    private boolean hasManualItem = false;

    @Column(name = "delivery_area", nullable = false, length = 200)
    private String deliveryArea;

    @Column(name = "pickup_area", nullable = false, length = 200)
    private String pickupArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level", nullable = false, length = 20)
    private UrgencyLevel urgencyLevel;

    /** Optional discount percentage offered by the carrier. */
    @Column(precision = 5, scale = 2)
    private BigDecimal discount;

    /** Ad copy to attract shippers — shown on the offer listing. */
    @Column(name = "special_note", columnDefinition = "TEXT")
    private String specialNote;

    @ElementCollection
    @CollectionTable(name = "offer_meetup_places", joinColumns = @JoinColumn(name = "offer_id"))
    @Column(name = "place", nullable = false)
    @Builder.Default
    private List<String> meetupPlaces = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "offer_payment_methods", joinColumns = @JoinColumn(name = "offer_id"))
    @Column(name = "payment_method", nullable = false, length = 100)
    @Builder.Default
    private List<String> paymentMethods = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OfferItem> offerItems = new ArrayList<>();

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
