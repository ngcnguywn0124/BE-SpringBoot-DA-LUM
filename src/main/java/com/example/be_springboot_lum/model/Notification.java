package com.example.be_springboot_lum.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bảng: notifications
 * Thông báo gửi tới người dùng (lưu DB + push WebSocket real-time).
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID notificationId;

    /** Người nhận thông báo */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Loại thông báo:
     * new_message | new_offer | transaction_update | product_sold |
     * product_expired | review_received | wishlist_update | new_follower |
     * admin_message | system
     */
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Người tạo ra sự kiện (có thể null với thông báo hệ thống) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    /** product | conversation | transaction | review | user */
    @Column(name = "related_entity_type", length = 30)
    private String relatedEntityType;

    @Column(name = "related_entity_id", columnDefinition = "uuid")
    private UUID relatedEntityId;

    /** Đường dẫn điều hướng phía FE khi click thông báo */
    @Column(name = "target_href", columnDefinition = "TEXT")
    private String targetHref;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
