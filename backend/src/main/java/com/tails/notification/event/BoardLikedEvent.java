package com.tails.notification.event;

public record BoardLikedEvent(Long boardOwnerId, Long likerId, Long boardId) {
}
