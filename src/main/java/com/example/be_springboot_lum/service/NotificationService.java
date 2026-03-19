package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    /**
     * Tạo & gửi thông báo tới user (lưu DB + push WebSocket).
     *
     * @param userId            Người nhận thông báo
     * @param type              Loại thông báo (vd: transaction_update)
     * @param title             Tiêu đề
     * @param content           Nội dung chi tiết
     * @param actorId           ID người gây ra sự kiện (null nếu là system)
     * @param relatedEntityType Loại entity liên quan: product | transaction | ...
     * @param relatedEntityId   UUID entity liên quan
     * @param targetHref        Đường dẫn FE khi click thông báo
     */
    NotificationResponse sendNotification(
            UUID userId,
            String type,
            String title,
            String content,
            UUID actorId,
            String relatedEntityType,
            UUID relatedEntityId,
            String targetHref
    );

    /** Danh sách thông báo của user, mới nhất trước */
    Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable);

    /** Số thông báo chưa đọc */
    long getUnreadCount(UUID userId);

    /** Đánh dấu một thông báo là đã đọc */
    NotificationResponse markAsRead(UUID userId, UUID notificationId);

    /** Đánh dấu tất cả thông báo của user là đã đọc */
    void markAllAsRead(UUID userId);
}
