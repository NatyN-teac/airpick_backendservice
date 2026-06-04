package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.MatchedItemStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MatchedItemStatusHistoryRepository extends JpaRepository<MatchedItemStatusHistory, UUID> {}
