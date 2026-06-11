package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.UserVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserVerificationRepository extends JpaRepository<UserVerification, UUID> {

    Optional<UserVerification> findByUserProfileId(UUID userProfileId);

    @Query("""
            SELECT uv FROM UserVerification uv
            JOIN FETCH uv.userProfile
            WHERE uv.veriffSessionId = :sessionId
            """)
    Optional<UserVerification> findByVeriffSessionIdWithProfile(@Param("sessionId") String sessionId);

    // Paging and counts for admin listing
    org.springframework.data.domain.Page<UserVerification> findByStatus(String status, org.springframework.data.domain.Pageable pageable);

    long countByStatus(String status);
}
