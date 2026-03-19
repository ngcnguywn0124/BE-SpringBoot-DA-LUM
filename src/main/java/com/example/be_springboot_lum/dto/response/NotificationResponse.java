package com.example.be_springboot_lum.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID notificationId;

    /** Loại thông báo */
    private String notificationType;

    private String title;
    private String content;

    /** Người gây ra sự kiện */
    private UUID actorId;
    private String actorName;
    private String actorAvatarUrl;

    private String imageUrl;

    /** Loại entity liên quan: product | conversation | transaction | review | user */
    private String relatedEntityType;
    private UUID relatedEntityId;

    /** Đường dẫn FE khi click thông báo */
    private String targetHref;

    private Boolean isRead;
    private OffsetDateTime readAt;
    private OffsetDateTime createdAt;
}
