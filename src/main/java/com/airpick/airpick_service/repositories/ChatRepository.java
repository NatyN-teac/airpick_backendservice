package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    @Query("SELECT c FROM Chat c WHERE c.match.id = :matchId")
    Optional<Chat> findByMatchId(@Param("matchId") UUID matchId);
}
