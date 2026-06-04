package com.airpick.airpick_service.dtos.input;

/**
 * Input DTO for sending a chat message.
 * Sent by the client over WebSocket to /app/chat/{matchId}/send.
 */
public record ChatMessageRequestDto(String content) {}
