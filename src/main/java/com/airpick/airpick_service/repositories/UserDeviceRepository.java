package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

    List<UserDevice> findAllByUserId(UUID userId);

    Optional<UserDevice> findByFcmToken(String fcmToken);

    @Query("SELECT d FROM UserDevice d WHERE d.user.id = :userId AND d.fcmToken = :fcmToken")
    Optional<UserDevice> findByUserIdAndFcmToken(@Param("userId") UUID userId,
                                                  @Param("fcmToken") String fcmToken);

    @Modifying
    @Query("DELETE FROM UserDevice d WHERE d.fcmToken = :fcmToken")
    void deleteByFcmToken(@Param("fcmToken") String fcmToken);

    @Modifying
    @Query("DELETE FROM UserDevice d WHERE d.user.id = :userId AND d.fcmToken = :fcmToken")
    void deleteByUserIdAndFcmToken(@Param("userId") UUID userId,
                                    @Param("fcmToken") String fcmToken);
}
