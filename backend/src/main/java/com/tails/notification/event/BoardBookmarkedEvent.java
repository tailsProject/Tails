package com.tails.notification.event;

public record BoardBookmarkedEvent(Long boardOwnerId, Long bookmarkerId, Long boardId) {
}
