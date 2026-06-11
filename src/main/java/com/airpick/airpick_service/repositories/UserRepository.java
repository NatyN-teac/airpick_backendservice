package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByIsActiveUserTrue();

    long countByIsBlockedTrue();

    java.util.List<com.airpick.airpick_service.models.User> findByRole_Name(String roleName);

    // Basic search for admin listing (searches email)
    org.springframework.data.domain.Page<com.airpick.airpick_service.models.User> findByEmailContainingIgnoreCase(String email, org.springframework.data.domain.Pageable pageable);
}
