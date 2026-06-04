package com.airpick.airpick_service.commons.configs;

import com.airpick.airpick_service.commons.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket configuration.
 * <p>
 * Clients connect to /ws (with SockJS fallback).
 * Messages sent to /app/... are routed to @MessageMapping methods.
 * Messages sent to /topic/... or /queue/... are delivered via the in-memory broker.
 * User-specific messages use /user/... prefix (Spring rewrites per connected principal).
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker handles /topic (broadcast) and /queue (user-specific)
        registry.enableSimpleBroker("/topic", "/queue");
        // Client sends messages to /app/... which routes to @MessageMapping handlers
        registry.setApplicationDestinationPrefixes("/app");
        // Spring rewrites /user/queue/... to /user/{principal}/queue/... per connected user
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // SockJS fallback for environments that block raw WebSocket
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Validate JWT on every STOMP CONNECT frame
        registration.interceptors(webSocketAuthInterceptor);
    }
}
