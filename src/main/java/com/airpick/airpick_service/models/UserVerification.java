package com.airpick.airpick_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_verifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false, unique = true)
    private UserProfile userProfile;

    @Column(name = "verification_requested_at")
    private LocalDateTime verificationRequestedAt;

    @Column(length = 50)
    private String status;

    @Column(name = "verification_message", columnDefinition = "TEXT")
    private String verificationMessage;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "is_denied", nullable = false)
    private boolean isDenied = false;
}
