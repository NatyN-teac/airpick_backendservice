package com.airpick.airpick_service.controllers;

import com.airpick.airpick_service.dtos.input.ChatMessageRequestDto;
import com.airpick.airpick_service.dtos.output.ChatMessageResponseDto;
import com.airpick.airpick_service.services.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket controller for real-time chat messaging.
 * <p>
 * Clients connect to ws://host/ws with a valid JWT in the Authorization header.
 * After connecting, they subscribe to the match chat topic and send messages
 * through the application destination.
 *
 * <pre>
 * Connect:     ws://host/api/v1/ws
 *              Header: Authorization: Bearer <jwt>
 *
 * Subscribe:   /topic/match/{matchId}/chat     — receive incoming messages
 * Subscribe:   /user/queue/notifications       — receive personal notifications
 * Subscribe:   /user/queue/errors             — receive send-error callbacks
 *
 * Send:        /app/chat/{matchId}/send        — { "content": "Hello" }
 * </pre>
 *
 * Chat rooms only exist after the carrier accepts a match. Attempting to send
 * a message before acceptance delivers an error to /user/queue/errors.
 *
 * -------------------------------------------------------------------------
 * FLUTTER INTEGRATION NOTES
 * -------------------------------------------------------------------------
 * Recommended package: stomp_dart_client (pub.dev/packages/stomp_dart_client)
 *
 * 1. CONNECT
 *    final client = StompClient(
 *      config: StompConfig.sockJS(
 *        url: 'https://your-api-host/api/v1/ws',
 *        onConnect: onConnectCallback,
 *        stompConnectHeaders: { 'Authorization': 'Bearer $jwtToken' },
 *        webSocketConnectHeaders: { 'Authorization': 'Bearer $jwtToken' },
 *      ),
 *    );
 *    client.activate();
 *
 * 2. SUBSCRIBE TO CHAT MESSAGES (open when chat screen is mounted)
 *    client.subscribe(
 *      destination: '/topic/match/$matchId/chat',
 *      callback: (frame) {
 *        final msg = ChatMessageResponseDto.fromJson(jsonDecode(frame.body!));
 *        // add msg to local chat list, trigger setState / notifyListeners
 *      },
 *    );
 *
 * 3. PUSH NOTIFICATIONS (FCM — handled outside WebSocket)
 *    Notifications (match accepted/rejected/cancelled, new proposal, new message badge)
 *    are delivered via Firebase Cloud Messaging, NOT over this WebSocket connection.
 *    Configure FirebaseMessaging in main.dart:
 *
 *    FirebaseMessaging.onMessage.listen((message) {
 *      // App is in foreground — suppress system tray, show in-app banner
 *      final data = message.data;
 *      // data keys: notificationId, type, title, body, refType, refId
 *      // route on data['type']:
 *      //   'MATCH_ACCEPTED'    → data['refType']='MATCH',         navigate to MatchDetailScreen(data['refId'])
 *      //   'MATCH_REJECTED'    → data['refType']='MATCH',         show rejection banner
 *      //   'MATCH_CANCELLED'   → data['refType']='MATCH',         update match status in UI
 *      //   'NEW_PROPOSAL'      → data['refType']='OFFER_REQUEST', show badge on request
 *      //   'PROPOSAL_ACCEPTED' → data['refType']='MATCH',         navigate to new match
 *      //   'NEW_MESSAGE'       → data['refType']='MATCH',         show badge on chat icon
 *    });
 *
 *    FirebaseMessaging.onMessageOpenedApp.listen((message) {
 *      // App was backgrounded, user tapped notification — deep link
 *      final data = message.data;
 *      switch (data['refType']) {
 *        case 'MATCH':         Navigator.push(MatchDetailScreen(data['refId']));
 *        case 'OFFER_REQUEST': Navigator.push(OfferRequestScreen(data['refId']));
 *      }
 *    });
 *
 *    // Register token on app start (call after login)
 *    final token = await FirebaseMessaging.instance.getToken();
 *    await api.post('/users/devices', { 'fcmToken': token, 'platform': 'ANDROID' or 'IOS' });
 *    FirebaseMessaging.instance.onTokenRefresh.listen((newToken) {
 *      api.post('/users/devices', { 'fcmToken': newToken, 'platform': ... });
 *    });
 *
 * 4. SEND A MESSAGE
 *    client.send(
 *      destination: '/app/chat/$matchId/send',
 *      body: jsonEncode({ 'content': messageText }),
 *    );
 *
 *    Subscribe to /user/queue/errors to catch send failures:
 *    client.subscribe(
 *      destination: '/user/queue/errors',
 *      callback: (frame) {
 *        final err = jsonDecode(frame.body!)['error'];
 *        // show error snackbar, re-enable send button
 *      },
 *    );
 *
 * 5. DISCONNECT (on logout or app background)
 *    client.deactivate();
 *
 * 6. RECONNECT STRATEGY
 *    StompConfig has reconnectDelay — set to Duration(seconds: 5) for
 *    automatic reconnection on network drop. Re-subscribe in onConnect callback
 *    so subscriptions are restored after reconnect.
 *
 * 7. LOAD HISTORY ON CHAT SCREEN OPEN
 *    Call REST GET /chats/match/{matchId} before activating the WS subscription.
 *    Render history first, then append live WS messages as they arrive.
 *    Call PATCH /chats/{chatId}/read when the user opens or focuses the screen.
 *
 * 8. MESSAGE MODEL (ChatMessageResponseDto fields)
 *    id, chatId, senderId, senderName, senderRole (CARRIER|SHIPPER),
 *    content, isRead, sentAt
 *
 * 9. FCM DATA PAYLOAD (message.data fields)
 *    notificationId, type, title, body, refType, refId
 *    → Call PATCH /notifications/{notificationId}/read after the user acts on it
 * -------------------------------------------------------------------------
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles an incoming chat message from an authenticated client.
     * Persists the message (inside a transaction in ChatService), then broadcasts
     * the saved message to all subscribers of the match chat topic.
     * Broadcasting happens AFTER the service call returns so the transaction is
     * already committed before any subscriber queries the REST endpoint.
     *
     * @param matchId   the match ID from the destination path
     * @param request   the message payload — must contain non-blank content
     * @param principal the authenticated sender (set by WebSocketAuthInterceptor)
     */
    @MessageMapping("/chat/{matchId}/send")
    public void sendMessage(
            @DestinationVariable String matchId,
            @Payload ChatMessageRequestDto request,
            Principal principal) {

        log.info("===>>> WS sendMessage HIT — match: {}, sender: {}, content: {}",
                matchId, principal != null ? principal.getName() : "NULL_PRINCIPAL", request);

        UUID matchUuid;
        try {
            matchUuid = UUID.fromString(matchId);
        } catch (IllegalArgumentException e) {
            log.warn("===>>> WS invalid matchId '{}': {}", matchId, e.getMessage());
            throw new IllegalArgumentException("Invalid match ID: " + matchId);
        }

        ChatMessageResponseDto response = chatService.sendMessage(matchUuid, principal.getName(), request);

        // Broadcast after the transaction commits so all subscribers see persisted data.
        messagingTemplate.convertAndSend("/topic/match/" + matchId + "/chat", response);
        log.info("===>>> WS message {} broadcast to /topic/match/{}/chat", response.id(), matchId);
    }

    /**
     * Catches any exception thrown during message handling and delivers an error frame
     * to the sender's personal error queue so the client knows the message was not saved.
     *
     * @param ex        the exception that was thrown
     * @param principal the sender
     * @return an error payload delivered to /user/queue/errors
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Map<String, String> handleException(Exception ex, Principal principal) {
        log.warn("WS message error for user {}: {}", principal != null ? principal.getName() : "unknown", ex.getMessage(), ex);
        return Map.of("error", ex.getMessage() != null ? ex.getMessage() : "Failed to send message");
    }
}
