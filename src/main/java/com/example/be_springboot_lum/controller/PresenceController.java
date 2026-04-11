package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.response.PresenceEvent;
import com.example.be_springboot_lum.service.PresenceService;
import com.example.be_springboot_lum.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${api.prefix}/presences")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PresenceEvent>>> getPresences(
            @RequestParam(required = false) String userIds) {
        if (userIds == null || userIds.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success("Thành công", List.of()));
        }

        List<UUID> uuidList = Arrays.stream(userIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::safeParseUUID)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                "Thành công",
                presenceService.getPresences(uuidList)));
    }

    @PatchMapping("/me/online")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PresenceEvent>> markMyOnline() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Đã cập nhật trạng thái hoạt động",
                presenceService.onUserConnected(currentUserId)));
    }

    @PatchMapping("/me/offline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PresenceEvent>> markMyOffline() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Đã cập nhật trạng thái ngoại tuyến",
                presenceService.onUserDisconnected(currentUserId, OffsetDateTime.now())));
    }

    @PostMapping("/me/heartbeat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PresenceEvent>> heartbeat() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Heartbeat thành công",
                presenceService.onUserConnected(currentUserId)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<PresenceEvent>> getPresence(@PathVariable UUID userId) {
        PresenceEvent event = presenceService.getPresence(userId)
                .orElseGet(() -> PresenceEvent.builder()
                        .userId(userId)
                        .online(false)
                        .lastSeenAt(null)
                        .build());
                        
        return ResponseEntity.ok(ApiResponse.success("Thành công", event));
    }

    private UUID safeParseUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
