package com.tails.notification.event;

public record CommentCreatedEvent(Long boardOwnerId, Long commenterId, Long boardId) {
}
