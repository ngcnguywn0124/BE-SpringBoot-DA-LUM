package com.example.be_springboot_lum.config;

import com.example.be_springboot_lum.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Listen WebSocket connect/disconnect để cập nhật presence.
 */
@Component
@RequiredArgsConstructor
public class WebSocketPresenceListener {

    private final PresenceService presenceService;

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user == null || user.getName() == null || user.getName().isBlank()) return;

        try {
            UUID userId = UUID.fromString(user.getName());
            presenceService.onUserConnected(userId);
        } catch (IllegalArgumentException ignored) {
            // ignore invalid principal
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user == null || user.getName() == null || user.getName().isBlank()) return;

        try {
            UUID userId = UUID.fromString(user.getName());
            presenceService.onUserDisconnected(userId, OffsetDateTime.now());
        } catch (IllegalArgumentException ignored) {
            // ignore invalid principal
        }
    }
}

