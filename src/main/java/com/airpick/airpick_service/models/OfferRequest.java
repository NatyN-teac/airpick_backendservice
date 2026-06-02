package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A sender-initiated request looking for a carrier to transport items.
 * <p>
 * {@code sourceCountry} and {@code destinationCountry} are mandatory.
 * {@code srcAirport} and {@code destAirport} are optional refinements.
 * <p>
 * Carriers browse open requests and submit {@link OfferProposal}s with their price.
 * When the sender accepts a proposal, an {@link Offer} is created with
 * {@link OfferSource#FROM_PROPOSAL} and a {@link Match} is created immediately.
 */
@Entity
@Table(name = "offer_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", nullable = false)
    private User shipper;

    @Column(name = "source_country", nullable = false, length = 100)
    private String sourceCountry;

    @Column(name = "destination_country", nullable = false, length = 100)
    private String destinationCountry;

    /** Optional — more precise than country alone. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "src_airport_id")
    private Airport srcAirport;

    /** Optional — more precise than country alone. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dest_airport_id")
    private Airport destAirport;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level", nullable = false, length = 20)
    private UrgencyLevel urgencyLevel;

    @Column(name = "special_note", columnDefinition = "TEXT")
    private String specialNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OfferRequestStatus status = OfferRequestStatus.OPEN;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "offerRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OfferRequestItem> items = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "offerRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OfferProposal> proposals = new ArrayList<>();

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
