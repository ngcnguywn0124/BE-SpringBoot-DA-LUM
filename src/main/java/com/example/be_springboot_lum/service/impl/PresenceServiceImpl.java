package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.response.PresenceEvent;
import com.example.be_springboot_lum.model.User;
import com.example.be_springboot_lum.repository.UserRepository;
import com.example.be_springboot_lum.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Lưu trạng thái online theo userId.
     * NOTE: in-memory; khi restart server sẽ reset. lastSeenAt được persist xuống DB.
     */
    private final ConcurrentMap<UUID, PresenceEvent> presence = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void onUserConnected(UUID userId) {
        PresenceEvent event = PresenceEvent.builder()
                .userId(userId)
                .online(true)
                .lastSeenAt(null)
                .build();

        presence.put(userId, event);
        publish(event);
    }

    @Override
    @Transactional
    public void onUserDisconnected(UUID userId, OffsetDateTime when) {
        OffsetDateTime ts = when != null ? when : OffsetDateTime.now();

        // Persist lastSeenAt
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastSeenAt(ts);
            userRepository.save(user);
        });

        PresenceEvent event = PresenceEvent.builder()
                .userId(userId)
                .online(false)
                .lastSeenAt(ts)
                .build();

        presence.put(userId, event);
        publish(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PresenceEvent> getPresence(UUID userId) {
        PresenceEvent inMemory = presence.get(userId);
        if (inMemory != null) {
            return Optional.of(inMemory);
        }

        return userRepository.findById(userId).map(User::getLastSeenAt).map(lastSeenAt ->
                PresenceEvent.builder()
                        .userId(userId)
                        .online(false)
                        .lastSeenAt(lastSeenAt)
                        .build()
        );
    }

    private void publish(PresenceEvent event) {
        // 1) Broadcast cho mọi client (FE tự filter theo userId trong conversation list)
        messagingTemplate.convertAndSend("/topic/presence", event);
        // 2) Topic theo user (phòng khi cần subscribe riêng)
        messagingTemplate.convertAndSend("/topic/presence/user-" + event.getUserId(), event);
    }
}

