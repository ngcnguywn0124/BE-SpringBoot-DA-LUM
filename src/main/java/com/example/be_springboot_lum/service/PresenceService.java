package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.response.PresenceEvent;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PresenceService {
    void onUserConnected(UUID userId);
    void onUserDisconnected(UUID userId, OffsetDateTime when);

    Optional<PresenceEvent> getPresence(UUID userId);
}

