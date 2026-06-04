package com.airpick.airpick_service.services;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.airpick.airpick_service.models.UserDevice;
import com.airpick.airpick_service.repositories.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Thin wrapper around Firebase Cloud Messaging.
 * <p>
 * Sends data-only FCM messages to all registered devices for a given user.
 * Data-only messages (no notification block) allow Flutter to handle
 * presentation consistently across foreground, background, and terminated states.
 * <p>
 * Stale tokens (FCM returns UNREGISTERED or INVALID_ARGUMENT) are automatically
 * removed from {@code user_devices} to prevent accumulation of dead tokens.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final UserDeviceRepository userDeviceRepository;

    /**
     * Sends an FCM data message to every registered device for the given user.
     *
     * @param userId  the recipient's user ID
     * @param data    key/value pairs included in the FCM data payload
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendToUser(UUID userId, Map<String, String> data) {
        List<UserDevice> devices = userDeviceRepository.findAllByUserId(userId);

        if (devices.isEmpty()) {
            log.debug("No registered devices for user {}, skipping FCM", userId);
            return;
        }

        for (UserDevice device : devices) {
            sendToDevice(device, data);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void sendToDevice(UserDevice device, Map<String, String> data) {
        Message message = Message.builder()
                .setToken(device.getFcmToken())
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .putHeader("apns-priority", "10")
                        .build())
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.debug("FCM message {} sent to device {} (user {})",
                    messageId, device.getId(), device.getUser().getId());
        } catch (FirebaseMessagingException e) {
            handleFcmError(e, device);
        }
    }

    private void handleFcmError(FirebaseMessagingException e, UserDevice device) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();

        if (errorCode == MessagingErrorCode.UNREGISTERED
                || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            log.warn("Stale FCM token for device {} (user {}) — removing. Error: {}",
                    device.getId(), device.getUser().getId(), errorCode);
            userDeviceRepository.deleteByFcmToken(device.getFcmToken());
        } else {
            log.error("FCM send failed for device {} (user {}): {} — {}",
                    device.getId(), device.getUser().getId(), errorCode, e.getMessage());
        }
    }
}
