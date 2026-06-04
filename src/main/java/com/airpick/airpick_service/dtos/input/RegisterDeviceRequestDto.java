package com.airpick.airpick_service.dtos.input;

import com.airpick.airpick_service.models.DevicePlatform;

/**
 * Input DTO for registering or refreshing a device's FCM token.
 * Flutter calls this endpoint on every app start and whenever Firebase rotates the token.
 */
public record RegisterDeviceRequestDto(
        String fcmToken,
        DevicePlatform platform,
        String deviceName
) {}
