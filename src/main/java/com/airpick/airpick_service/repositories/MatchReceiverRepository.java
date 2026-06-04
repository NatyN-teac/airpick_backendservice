package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.MatchReceiver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchReceiverRepository extends JpaRepository<MatchReceiver, UUID> {

    @Query("SELECT r FROM MatchReceiver r WHERE r.match.id = :matchId")
    Optional<MatchReceiver> findByMatchId(@Param("matchId") UUID matchId);

    /** Returns all receivers ever registered by a shipper — useful for a future saved-receivers feature. */
    @Query("SELECT r FROM MatchReceiver r WHERE r.shipper.id = :shipperId ORDER BY r.createdAt DESC")
    List<MatchReceiver> findAllByShipperId(@Param("shipperId") UUID shipperId);
}
