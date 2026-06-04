package com.airpick.airpick_service.repositories;

import com.airpick.airpick_service.models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    @Query("SELECT m FROM ChatMessage m WHERE m.chat.id = :chatId ORDER BY m.sentAt ASC")
    List<ChatMessage> findAllByChatId(@Param("chatId") UUID chatId);

    @Query("SELECT m FROM ChatMessage m WHERE m.chat.id = :chatId AND m.isRead = false AND m.sender.id != :userId")
    List<ChatMessage> findUnreadByChatIdForUser(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chat.id = :chatId AND m.isRead = false AND m.sender.id != :userId")
    int countUnreadByChatIdForUser(@Param("chatId") UUID chatId, @Param("userId") UUID userId);
}
