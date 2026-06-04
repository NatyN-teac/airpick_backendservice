package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.MatchedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchedItemRepository extends JpaRepository<MatchedItem, UUID> {

    @Query("SELECT mi FROM MatchedItem mi WHERE mi.match.id = :matchId")
    List<MatchedItem> findAllByMatchId(@Param("matchId") UUID matchId);
}
