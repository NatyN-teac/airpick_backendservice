package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    @Query("SELECT m FROM Match m WHERE m.shipper.id = :shipperId ORDER BY m.createdAt DESC")
    List<Match> findAllByShipperId(@Param("shipperId") UUID shipperId);

    @Query("SELECT m FROM Match m WHERE m.carrier.id = :carrierId ORDER BY m.createdAt DESC")
    List<Match> findAllByCarrierId(@Param("carrierId") UUID carrierId);

    @Query("SELECT m FROM Match m WHERE m.offer.id = :offerId ORDER BY m.createdAt DESC")
    List<Match> findAllByOfferId(@Param("offerId") UUID offerId);
}
