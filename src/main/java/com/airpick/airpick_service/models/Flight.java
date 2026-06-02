package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a flight journey created by a user.
 * <p>
 * A flight is a container that groups one or more {@link FlightLeg} entries.
 * A {@link FlightType#ONE_WAY} flight has exactly one leg; a
 * {@link FlightType#ROUND_TRIP} flight has two legs where the return leg
 * may depart from a different airport than the outbound leg arrived at.
 * <p>
 * {@code isBooked} is set to {@code true} once this flight is linked to a
 * shipment or carrier arrangement and is no longer available for matching.
 * {@code isDeleted} is set to {@code true} on soft delete — the record is
 * preserved but excluded from all queries.
 */
@Entity
@Table(name = "flights")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "flight_type", nullable = false, length = 20)
    private FlightType flightType;

    /** {@code true} when this flight is linked and no longer available. */
    @Column(name = "is_booked", nullable = false)
    private boolean isBooked = false;

    /** Soft delete flag — excluded from all queries when {@code true}. */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("legOrder ASC")
    @Builder.Default
    private List<FlightLeg> legs = new ArrayList<>();

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
