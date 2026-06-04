package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.models.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponseDto(
        UUID id,
        String type,
        String title,
        String body,
        boolean isRead,
        String refType,
        UUID refId,
        LocalDateTime createdAt
) {
    public static NotificationResponseDto from(Notification n) {
        return new NotificationResponseDto(
                n.getId(),
                n.getType().name(),
                n.getTitle(),
                n.getBody(),
                n.isRead(),
                n.getRefType() != null ? n.getRefType().name() : null,
                n.getRefId(),
                n.getCreatedAt()
        );
    }
}
