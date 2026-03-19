package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.dto.response.NotificationResponse;
import com.example.be_springboot_lum.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST API cho Thông báo (Notification).
 *
 * NOTE: Dùng @RequestHeader("User-Id") như các controller khác trong dự án.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * GET /api/notifications
     * Danh sách thông báo (phân trang, mới nhất trước).
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestHeader("User-Id") UUID currentUserId,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotifications(currentUserId, pageable));
    }

    /**
     * GET /api/notifications/unread-count
     * Số thông báo chưa đọc (dùng cho badge icon).
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("User-Id") UUID currentUserId) {
        long count = notificationService.getUnreadCount(currentUserId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * PUT /api/notifications/{notificationId}/read
     * Đánh dấu một thông báo là đã đọc.
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @RequestHeader("User-Id") UUID currentUserId,
            @PathVariable UUID notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(currentUserId, notificationId));
    }

    /**
     * PUT /api/notifications/read-all
     * Đánh dấu tất cả thông báo của user là đã đọc.
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader("User-Id") UUID currentUserId) {
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.noContent().build();
    }
}
