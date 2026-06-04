package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The person who will physically receive the matched items at the destination.
 * <p>
 * Created when a shipper indicates {@code receiverNeeded = true} on a match request.
 * {@code isActive} is false until the carrier accepts the match, at which point the
 * receiver is confirmed as the legitimate recipient for the delivery.
 * <p>
 * {@code shipper} is the primary sender who registered this receiver — the person
 * responsible for the delivery and accountable for the provided information.
 */
@Entity
@Table(name = "match_receivers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchReceiver {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private Match match;

    /** The shipper (sender) who submitted this receiver — the primary accountable party. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", nullable = false)
    private User shipper;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 30)
    private String phone;

    /** URL to the receiver's government-issued photo ID, uploaded by the shipper. */
    @Column(name = "photo_id_url", nullable = false, columnDefinition = "TEXT")
    private String photoIdUrl;

    /**
     * False until the carrier accepts the match, at which point the receiver
     * is confirmed active for this delivery.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

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
