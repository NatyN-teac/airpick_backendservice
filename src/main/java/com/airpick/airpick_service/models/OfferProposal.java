package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A carrier's price proposal in response to a sender's {@link OfferRequest}.
 * <p>
 * When the sender accepts a proposal:
 * <ol>
 *   <li>This proposal moves to {@link OfferProposalStatus#ACCEPTED}.</li>
 *   <li>All other proposals on the same request move to {@link OfferProposalStatus#REJECTED}.</li>
 *   <li>An {@link Offer} with {@link OfferSource#FROM_PROPOSAL} is created.</li>
 *   <li>A {@link Match} with {@link MatchStatus#ACCEPTED} is created immediately.</li>
 * </ol>
 * // TODO: notify carrier when their proposal is accepted — push notification, implement in next phase
 */
@Entity
@Table(name = "offer_proposals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_request_id", nullable = false)
    private OfferRequest offerRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id", nullable = false)
    private User carrier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OfferProposalStatus status = OfferProposalStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String note;

    /** Optional — falls back to offerRequest.destinationCountry when null. */
    @Column(name = "delivery_area", length = 200)
    private String deliveryArea;

    /** Optional — falls back to offerRequest.sourceCountry when null. */
    @Column(name = "pickup_area", length = 200)
    private String pickupArea;

    @Column(precision = 5, scale = 2)
    private BigDecimal discount;

    @ElementCollection
    @CollectionTable(name = "offer_proposal_meetup_places",
            joinColumns = @JoinColumn(name = "offer_proposal_id"))
    @Column(name = "place", nullable = false)
    @Builder.Default
    private List<String> meetupPlaces = new ArrayList<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "offer_proposal_payment_methods",
            joinColumns = @JoinColumn(name = "offer_proposal_id"))
    @Column(name = "payment_method", nullable = false, length = 30)
    @Builder.Default
    private List<PaymentMethod> paymentMethods = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OfferProposalItem> items = new ArrayList<>();

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
