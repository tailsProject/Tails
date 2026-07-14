package com.tails.notification;

// 알림 종류. 트리거 연동 커밋에서 이벤트 발행/구독으로 채워짐
public enum NotificationType {
    COMMENT,
    LIKE,
    BOOKMARK
}
