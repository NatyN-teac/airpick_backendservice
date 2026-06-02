package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Represents a single segment (leg) of a {@link Flight}.
 * <p>
 * A {@link FlightType#ONE_WAY} flight has exactly one leg ({@code legOrder = 1}).
 * A {@link FlightType#ROUND_TRIP} flight has two legs:
 * <ul>
 *   <li>{@code legOrder = 1} — outbound leg</li>
 *   <li>{@code legOrder = 2} — return leg, which may depart from a different
 *       airport than the outbound leg arrived at</li>
 * </ul>
 * Both {@code srcAirport} and {@code destAirport} reference the centralised
 * {@link Airport} entity controlled by the admin.
 */
@Entity
@Table(name = "flight_legs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightLeg {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    /**
     * Position of this leg within the flight.
     * 1 = outbound, 2 = return.
     */
    @Column(name = "leg_order", nullable = false)
    private int legOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "src_airport_id", nullable = false)
    private Airport srcAirport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dest_airport_id", nullable = false)
    private Airport destAirport;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "arrival_date", nullable = false)
    private LocalDate arrivalDate;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

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
