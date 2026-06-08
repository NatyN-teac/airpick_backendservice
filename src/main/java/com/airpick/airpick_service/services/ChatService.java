package com.airpick.airpick_service.services;

import com.airpick.airpick_service.dtos.input.ChatMessageRequestDto;
import com.airpick.airpick_service.dtos.output.ChatMessageResponseDto;
import com.airpick.airpick_service.dtos.output.ChatRoomResponseDto;
import com.airpick.airpick_service.models.Chat;
import com.airpick.airpick_service.models.ChatMessage;
import com.airpick.airpick_service.models.Match;
import com.airpick.airpick_service.models.User;
import com.airpick.airpick_service.repositories.ChatMessageRepository;
import com.airpick.airpick_service.repositories.ChatRepository;
import com.airpick.airpick_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for chat messaging within a match.
 * <p>
 * Messages are persisted to the database and broadcast in real-time via WebSocket.
 * Only the carrier and shipper of the match may send or read messages.
 * The chat room is created when the carrier accepts a match.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Persists a message sent by the authenticated user.
     * The caller is responsible for broadcasting the returned DTO over WebSocket
     * after this method returns (i.e. after the transaction commits).
     *
     * @param matchId     the ID of the match whose chat this message belongs to
     * @param senderEmail the authenticated sender's email
     * @param request     the message content
     * @return the saved message as a response DTO
     */
    @Transactional
    public ChatMessageResponseDto sendMessage(UUID matchId, String senderEmail, ChatMessageRequestDto request) {
        log.info("===>>> ChatService.sendMessage called — match: {}, sender: {}", matchId, senderEmail);

        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        User sender = resolveUser(senderEmail);
        Chat chat = resolveChatForMatch(matchId);
        Match match = chat.getMatch();

        validateParticipant(match, sender);

        ChatMessage message = ChatMessage.builder()
                .chat(chat)
                .sender(sender)
                .content(request.content().trim())
                .isRead(false)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        log.debug("Message {} saved for match {}", saved.getId(), matchId);

        ChatMessageResponseDto response = ChatMessageResponseDto.from(saved, match);

        // Push notification in a best-effort fashion — a failure must not roll back the message save.
        try {
            notificationService.notifyNewMessage(match, senderEmail);
        } catch (Exception e) {
            log.warn("NEW_MESSAGE notification failed for match {} — message is persisted, notification skipped: {}",
                    matchId, e.getMessage(), e);
        }

        return response;
    }

    /**
     * Returns the full chat room context for a match: participant info, receiver info,
     * full message history, and the caller's unread count.
     * Only the carrier and shipper of the match may access the chat.
     *
     * @param matchId   the ID of the match
     * @param userEmail the authenticated user's email
     * @return the chat room DTO
     */
    @Transactional(readOnly = true)
    public ChatRoomResponseDto getChatRoom(UUID matchId, String userEmail) {
        User user = resolveUser(userEmail);
        Chat chat = resolveChatForMatch(matchId);
        Match match = chat.getMatch();

        validateParticipant(match, user);

        List<ChatMessage> messages = chatMessageRepository.findAllByChatId(chat.getId());
        int unreadCount = chatMessageRepository.countUnreadByChatIdForUser(chat.getId(), user.getId());

        List<ChatMessageResponseDto> messageDtos = messages.stream()
                .map(m -> ChatMessageResponseDto.from(m, match))
                .toList();

        log.info("Chat room fetched for match {} by user {}", matchId, userEmail);
        return ChatRoomResponseDto.from(chat, match, messageDtos, unreadCount);
    }

    /**
     * Marks all unread messages in a chat as read for the calling user.
     * Only marks messages sent by the *other* participant — you can't mark your own as read.
     *
     * @param chatId    the ID of the chat
     * @param userEmail the authenticated user's email
     */
    @Transactional
    public void markMessagesAsRead(UUID chatId, String userEmail) {
        User user = resolveUser(userEmail);

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + chatId));

        validateParticipant(chat.getMatch(), user);

        List<ChatMessage> unread = chatMessageRepository.findUnreadByChatIdForUser(chatId, user.getId());
        unread.forEach(m -> m.setRead(true));
        chatMessageRepository.saveAll(unread);

        log.info("Marked {} messages as read in chat {} for user {}", unread.size(), chatId, userEmail);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    private Chat resolveChatForMatch(UUID matchId) {
        return chatRepository.findByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Chat not found for match: " + matchId +
                        ". Chat is only created after the carrier accepts the match."));
    }

    private void validateParticipant(Match match, User user) {
        boolean isParticipant = match.getCarrier().getId().equals(user.getId())
                || match.getShipper().getId().equals(user.getId());
        if (!isParticipant) {
            log.warn("User {} attempted to access chat for match {} without being a participant",
                    user.getId(), match.getId());
            throw new IllegalArgumentException("Chat not found");
        }
    }
}
