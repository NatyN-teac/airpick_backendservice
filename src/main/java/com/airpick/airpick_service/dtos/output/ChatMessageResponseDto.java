package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.models.ChatMessage;
import com.airpick.airpick_service.models.Match;
import com.airpick.airpick_service.models.UserProfile;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight message DTO used for both WebSocket broadcast and REST history.
 * Includes denormalized senderName and senderRole so the frontend can render
 * message bubbles without an additional lookup.
 */
public record ChatMessageResponseDto(
        UUID id,
        UUID chatId,
        UUID senderId,
        String senderName,
        String senderRole,
        String content,
        boolean isRead,
        LocalDateTime sentAt
) {
    public static ChatMessageResponseDto from(ChatMessage msg, Match match) {
        boolean isCarrier = msg.getSender().getId().equals(match.getCarrier().getId());
        String role = isCarrier ? "CARRIER" : "SHIPPER";

        UserProfile profile = msg.getSender().getUserProfile();
        String name = profile != null
                ? (profile.getFirstName() != null ? profile.getFirstName() : "") +
                  (profile.getLastName()  != null ? " " + profile.getLastName() : "")
                : msg.getSender().getEmail();

        return new ChatMessageResponseDto(
                msg.getId(),
                msg.getChat().getId(),
                msg.getSender().getId(),
                name.isBlank() ? msg.getSender().getEmail() : name.trim(),
                role,
                msg.getContent(),
                msg.isRead(),
                msg.getSentAt()
        );
    }
}
