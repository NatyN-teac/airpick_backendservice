package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reference item that carriers can carry and shippers can request.
 * <p>
 * Admin-seeded items are always approved ({@code isApproved = true}, {@code createdBy = null}).
 * Carrier-submitted items start with {@code isApproved = false} and require admin approval
 * before they are selectable in offers.
 */
@Entity
@Table(name = "items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_type", nullable = false, length = 50)
    private MeasurementType measurementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_unit", nullable = false, length = 50)
    private MeasurementUnit measurementUnit;

    /** Reserved for future enforcement — max quantity allowed per shipment. */
    @Column(name = "max_allowed", precision = 10, scale = 2)
    private BigDecimal maxAllowed;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /** false until admin approves; always true for admin-seeded items. */
    @Column(name = "is_approved", nullable = false)
    private boolean isApproved = false;

    /** null for admin-seeded items; FK to the carrier who submitted this item. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

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
