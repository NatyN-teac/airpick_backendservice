package com.airpick.airpick_service.dtos.output;

import com.airpick.airpick_service.models.Chat;
import com.airpick.airpick_service.models.Match;
import com.airpick.airpick_service.models.MatchReceiver;
import com.airpick.airpick_service.models.User;
import com.airpick.airpick_service.models.UserProfile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full chat room context returned on room open.
 * Loaded once by the frontend; individual messages arrive via WebSocket.
 */
public record ChatRoomResponseDto(
        UUID chatId,
        UUID matchId,
        String matchStatus,
        ParticipantDto carrier,
        ParticipantDto shipper,
        ReceiverInfoDto receiver,
        int totalUnreadCount,
        List<ChatMessageResponseDto> messages,
        LocalDateTime createdAt
) {
    public record ParticipantDto(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String profilePictureUrl
    ) {
        public static ParticipantDto from(User user) {
            UserProfile p = user.getUserProfile();
            return new ParticipantDto(
                    user.getId(),
                    p != null ? p.getFirstName() : null,
                    p != null ? p.getLastName()  : null,
                    user.getEmail(),
                    p != null ? p.getProfilePictureUrl() : null
            );
        }
    }

    public record ReceiverInfoDto(
            boolean receiverNeeded,
            String firstName,
            String lastName,
            String phone,
            String photoIdUrl,
            boolean isActive
    ) {
        /** receiverNeeded = false — shipper is the receiver, no third-party info. */
        public static ReceiverInfoDto shipperIsReceiver() {
            return new ReceiverInfoDto(false, null, null, null, null, true);
        }

        public static ReceiverInfoDto from(MatchReceiver r) {
            return new ReceiverInfoDto(
                    true,
                    r.getFirstName(),
                    r.getLastName(),
                    r.getPhone(),
                    r.getPhotoIdUrl(),
                    r.isActive()
            );
        }
    }

    public static ChatRoomResponseDto from(Chat chat, Match match,
                                           List<ChatMessageResponseDto> messages,
                                           int unreadCount) {
        ReceiverInfoDto receiverInfo = match.isReceiverNeeded() && match.getReceiver() != null
                ? ReceiverInfoDto.from(match.getReceiver())
                : ReceiverInfoDto.shipperIsReceiver();

        return new ChatRoomResponseDto(
                chat.getId(),
                match.getId(),
                match.getStatus().name(),
                ParticipantDto.from(match.getCarrier()),
                ParticipantDto.from(match.getShipper()),
                receiverInfo,
                unreadCount,
                messages,
                chat.getCreatedAt()
        );
    }
}
