package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a physical device registered by a user for FCM push notifications.
 * <p>
 * One user can have multiple active devices (e.g. phone + tablet).
 * Each device has a unique FCM token issued by Firebase on the client side.
 * Tokens are refreshed by the client on app start and whenever Firebase rotates them.
 * Stale tokens (FCM returns UNREGISTERED) are deleted automatically by
 * {@link com.airpick.airpick_service.services.PushNotificationService}.
 */
@Entity
@Table(name = "user_devices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "fcm_token", nullable = false, unique = true, length = 512)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform;

    /** Optional human-readable label sent by the client (e.g. "iPhone 14", "Samsung S23"). */
    @Column(name = "device_name", length = 100)
    private String deviceName;

    /** Updated every time the client re-registers the same token. */
    @Column(name = "last_active", nullable = false)
    private LocalDateTime lastActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastActive = LocalDateTime.now();
    }
}
