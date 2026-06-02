package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.Mode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModeRepository extends JpaRepository<Mode, Long> {

    Optional<Mode> findByName(String name);
}
