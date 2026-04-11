package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.response.PresenceEvent;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import java.util.List;

public interface PresenceService {
    PresenceEvent onUserConnected(UUID userId);
    PresenceEvent onUserDisconnected(UUID userId, OffsetDateTime when);

    Optional<PresenceEvent> getPresence(UUID userId);
    List<PresenceEvent> getPresences(List<UUID> userIds);
}

