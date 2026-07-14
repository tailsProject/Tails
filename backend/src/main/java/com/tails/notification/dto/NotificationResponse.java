package com.tails.notification.dto;

import com.tails.notification.Notification;
import com.tails.notification.NotificationType;
import java.time.LocalDateTime;
import lombok.Getter;

// 알림 응답 DTO
@Getter
public class NotificationResponse {

    private final Long notificationId;
    private final NotificationType type;
    private final String content;
    private final Long targetId;
    private final boolean read;
    private final LocalDateTime createdAt;

    private NotificationResponse(Long notificationId, NotificationType type, String content,
            Long targetId, boolean read, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.type = type;
        this.content = content;
        this.targetId = targetId;
        this.read = read;
        this.createdAt = createdAt;
    }

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getType(),
                notification.getContent(),
                notification.getTargetId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
