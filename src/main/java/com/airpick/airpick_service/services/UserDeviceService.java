package com.airpick.airpick_service.services;

import com.airpick.airpick_service.dtos.input.RegisterDeviceRequestDto;
import com.airpick.airpick_service.models.User;
import com.airpick.airpick_service.models.UserDevice;
import com.airpick.airpick_service.repositories.UserDeviceRepository;
import com.airpick.airpick_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Manages device FCM token registration for push notification delivery.
 * <p>
 * Flutter calls {@code POST /users/devices} on every app start (token may have rotated).
 * Flutter calls {@code DELETE /users/devices} on logout to stop receiving notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;

    /**
     * Registers or refreshes an FCM token for the authenticated user.
     * <p>
     * If the token already exists for this user, {@code lastActive} is updated.
     * If the token is new, a new device row is inserted.
     * If the token exists under a different user (e.g. shared test device), it is
     * re-assigned to the current user — last authenticated user owns the token.
     */
    @Transactional
    public void registerDevice(String email, RegisterDeviceRequestDto dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        userDeviceRepository.findByFcmToken(dto.fcmToken())
                .ifPresentOrElse(
                        existing -> {
                            // Token exists — update owner and refresh last_active
                            existing.setUser(user);
                            existing.setLastActive(LocalDateTime.now());
                            if (dto.deviceName() != null) existing.setDeviceName(dto.deviceName());
                            userDeviceRepository.save(existing);
                            log.info("FCM token refreshed for user {}", user.getId());
                        },
                        () -> {
                            // New token — insert device row
                            UserDevice device = UserDevice.builder()
                                    .user(user)
                                    .fcmToken(dto.fcmToken())
                                    .platform(dto.platform())
                                    .deviceName(dto.deviceName())
                                    .lastActive(LocalDateTime.now())
                                    .build();
                            userDeviceRepository.save(device);
                            log.info("New device registered for user {} (platform: {})",
                                    user.getId(), dto.platform());
                        }
                );
    }

    /**
     * Removes the given FCM token on logout so the device stops receiving notifications.
     */
    @Transactional
    public void unregisterDevice(String email, String fcmToken) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        userDeviceRepository.deleteByUserIdAndFcmToken(user.getId(), fcmToken);
        log.info("Device token unregistered for user {}", user.getId());
    }
}
