package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A persisted notification delivered to a user.
 * <p>
 * Every notification is saved here before being pushed via FCM so that:
 * <ul>
 *   <li>Users have a reliable notification inbox regardless of device state.</li>
 *   <li>Missed notifications (device offline, token stale) are recoverable on next app open.</li>
 *   <li>Unread counts and read/unread state are tracked server-side.</li>
 * </ul>
 * <p>
 * {@code refType} + {@code refId} form the deep-link payload the mobile app uses to
 * navigate to the relevant screen when the user taps the notification.
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    /**
     * The entity type this notification refers to.
     * Used by the mobile app to determine which screen to navigate to.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", length = 30)
    private RefType refType;

    /**
     * The ID of the entity this notification refers to.
     * Combined with {@code refType} forms the deep-link: e.g. MATCH + matchId.
     */
    @Column(name = "ref_id")
    private UUID refId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
