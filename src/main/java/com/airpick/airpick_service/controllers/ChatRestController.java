package com.airpick.airpick_service.controllers;

import com.airpick.airpick_service.commons.security.UserDetailsImpl;
import com.airpick.airpick_service.dtos.output.ApiResponseDto;
import com.airpick.airpick_service.dtos.output.ChatRoomResponseDto;
import com.airpick.airpick_service.services.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST endpoints for chat history and read-state management.
 * Real-time messaging is handled by {@link ChatWebSocketController} over WebSocket.
 */
@Tag(
        name = "Chat",
        description = "Chat room access and message history. " +
                      "A chat room is created automatically when a carrier accepts a match. " +
                      "Only the carrier and shipper of the match can access the chat. " +
                      "Real-time messaging is handled over WebSocket (STOMP) — " +
                      "connect to /ws and subscribe to /topic/match/{matchId}/chat. " +
                      "Use these REST endpoints to load history on app open and to mark messages as read."
)
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    @Operation(
            summary = "Get chat room for a match",
            description = "Returns the full chat room context: carrier info, shipper info, receiver info " +
                          "(null if shipper is the receiver), full message history ordered by sentAt ascending, " +
                          "and the unread message count for the calling user. " +
                          "Only accessible to the carrier and shipper of the match. " +
                          "Chat room only exists after the carrier has accepted the match."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chat room retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Chat not found — match may not have been accepted yet, " +
                                                             "or the caller is not a participant",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/match/{matchId}")
    public ResponseEntity<ApiResponseDto<ChatRoomResponseDto>> getChatRoom(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "ID of the match whose chat to open") @PathVariable UUID matchId) {
        return ResponseEntity.ok(ApiResponseDto.ok(
                chatService.getChatRoom(matchId, userDetails.getUsername())));
    }

    @Operation(
            summary = "Mark all messages as read",
            description = "Marks all unread messages in the chat as read for the calling user. " +
                          "Only marks messages sent by the other participant — " +
                          "you cannot mark your own messages as read. " +
                          "Call this when the user opens or focuses the chat window."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages marked as read",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Chat not found or caller is not a participant",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PatchMapping("/{chatId}/read")
    public ResponseEntity<ApiResponseDto<Void>> markMessagesAsRead(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "ID of the chat") @PathVariable UUID chatId) {
        chatService.markMessagesAsRead(chatId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponseDto.ok());
    }
}
