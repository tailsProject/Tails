package com.tails.notification.event;

public record CommentRepliedEvent(Long parentAuthorId, Long replierId, Long boardId) {
}
