package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Lấy danh sách thông báo của user, mới nhất trước */
    Page<Notification> findByUserUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /** Đếm số thông báo chưa đọc */
    long countByUserUserIdAndIsReadFalse(UUID userId);

    /** Đánh dấu tất cả thông báo của user là đã đọc */
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.isRead = true, n.readAt = :readAt
        WHERE n.user.userId = :userId AND n.isRead = false
        """)
    int markAllAsRead(@Param("userId") UUID userId, @Param("readAt") OffsetDateTime readAt);
}
