package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_verifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false, unique = true)
    private UserProfile userProfile;

    @Column(name = "verification_requested_at")
    private LocalDateTime verificationRequestedAt;

    @Column(length = 50)
    private String status;

    @Column(name = "verification_message", columnDefinition = "TEXT")
    private String verificationMessage;

    /** Veriff verification session ID returned by POST /v1/sessions. */
    @Column(name = "veriff_session_id", length = 255)
    private String veriffSessionId;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "is_denied", nullable = false)
    private boolean isDenied = false;
}
