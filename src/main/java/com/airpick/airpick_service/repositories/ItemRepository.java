package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    @Query("SELECT i FROM Item i WHERE i.isActive = true AND i.isApproved = true ORDER BY i.category ASC, i.name ASC")
    List<Item> findAllActiveApproved();

    @Query("SELECT i FROM Item i WHERE i.id = :id AND i.isActive = true AND i.isApproved = true")
    Optional<Item> findActiveApprovedById(@Param("id") UUID id);

    @Query("SELECT i FROM Item i WHERE i.isApproved = false ORDER BY i.createdAt DESC")
    List<Item> findAllPendingApproval();
}
