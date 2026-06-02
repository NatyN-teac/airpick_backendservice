package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.Mode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModeRepository extends JpaRepository<Mode, UUID> {

    Optional<Mode> findByName(String name);
}
