package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Append-only audit log of every {@link MatchedItemStatus} transition on a {@link MatchedItem}.
 * <p>
 * Provides the delivery timeline shown to the sender on the front end
 * (e.g. matched → collected → in transit → delivered).
 * Never updated after creation. {@code previousStatus} is null on the first entry.
 */
@Entity
@Table(name = "matched_item_status_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchedItemStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_item_id", nullable = false)
    private MatchedItem matchedItem;

    /** Null on the first history entry when the matched item is created. */
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private MatchedItemStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private MatchedItemStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    /** Optional note, e.g. "Delayed at customs" or "Carrier cancelled before pickup". */
    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
