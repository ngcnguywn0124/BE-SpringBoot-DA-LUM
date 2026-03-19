package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.response.NotificationResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.Notification;
import com.example.be_springboot_lum.model.User;
import com.example.be_springboot_lum.repository.NotificationRepository;
import com.example.be_springboot_lum.repository.UserRepository;
import com.example.be_springboot_lum.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public NotificationResponse sendNotification(
            UUID userId,
            String type,
            String title,
            String content,
            UUID actorId,
            String relatedEntityType,
            UUID relatedEntityId,
            String targetHref
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        User actor = null;
        if (actorId != null) {
            actor = userRepository.findById(actorId).orElse(null);
        }

        Notification notification = Notification.builder()
                .user(user)
                .notificationType(type)
                .title(title)
                .content(content)
                .actor(actor)
                .imageUrl(actor != null ? actor.getAvatarUrl() : null)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .targetHref(targetHref)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);

        NotificationResponse response = mapToResponse(notification);

        // Push real-time qua WebSocket — cùng pattern với chat
        try {
            messagingTemplate.convertAndSend(
                    "/topic/user-" + userId + "/notifications",
                    response
            );
        } catch (Exception e) {
            log.warn("Could not push notification via WebSocket for user {}: {}", userId, e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable) {
        return notificationRepository
                .findByUserUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(OffsetDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return mapToResponse(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId, OffsetDateTime.now());
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private NotificationResponse mapToResponse(Notification n) {
        NotificationResponse.NotificationResponseBuilder builder = NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .notificationType(n.getNotificationType())
                .title(n.getTitle())
                .content(n.getContent())
                .imageUrl(n.getImageUrl())
                .relatedEntityType(n.getRelatedEntityType())
                .relatedEntityId(n.getRelatedEntityId())
                .targetHref(n.getTargetHref())
                .isRead(n.getIsRead())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt());

        if (n.getActor() != null) {
            builder.actorId(n.getActor().getUserId())
                   .actorName(n.getActor().getFullName())
                   .actorAvatarUrl(n.getActor().getAvatarUrl());
        }

        return builder.build();
    }
}
