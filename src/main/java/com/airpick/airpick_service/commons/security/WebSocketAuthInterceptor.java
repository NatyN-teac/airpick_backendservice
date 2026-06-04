package com.airpick.airpick_service.commons.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Intercepts STOMP CONNECT frames and validates the JWT from the Authorization header.
 * Sets the authenticated {@link java.security.Principal} on the STOMP session so that
 * {@code @MessageMapping} methods receive a valid principal and
 * {@code SimpMessagingTemplate.convertAndSendToUser} can route user-specific messages.
 * <p>
 * Clients must include the header: {@code Authorization: Bearer <jwt>} on connect.
 * Connections with missing or invalid tokens are rejected.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message; // only validate on CONNECT
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("WebSocket CONNECT rejected — missing or malformed Authorization header");
            throw new IllegalArgumentException("Missing Authorization header on WebSocket CONNECT");
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            log.warn("WebSocket CONNECT rejected — invalid JWT");
            throw new IllegalArgumentException("Invalid JWT token on WebSocket CONNECT");
        }

        String email = jwtUtil.extractEmail(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        accessor.setUser(auth);
        log.debug("WebSocket CONNECT authenticated for user: {}", email);

        return message;
    }
}
