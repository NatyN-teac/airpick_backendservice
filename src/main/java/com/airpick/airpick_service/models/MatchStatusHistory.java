package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Append-only audit log of every {@link MatchStatus} transition on a {@link Match}.
 * <p>
 * Never updated after creation. {@code previousStatus} is null on the first entry.
 */
@Entity
@Table(name = "match_status_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    /** Null on the first history entry when the match is created. */
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private MatchStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private MatchStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    /** Optional reason or note for the status change, e.g. "Carrier cancelled before pickup". */
    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
