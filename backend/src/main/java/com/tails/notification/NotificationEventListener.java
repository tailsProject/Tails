package com.tails.notification;

import com.tails.notification.event.BoardBookmarkedEvent;
import com.tails.notification.event.BoardLikedEvent;
import com.tails.notification.event.CommentCreatedEvent;
import com.tails.notification.event.CommentRepliedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Board/Comment/Bookmark 이벤트를 구독해 알림으로 변환
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentCreatedEvent event) {
        notificationService.create(event.boardOwnerId(), NotificationType.COMMENT,
                "회원님의 게시글에 새 댓글이 달렸습니다.", event.boardId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentReplied(CommentRepliedEvent event) {
        notificationService.create(event.parentAuthorId(), NotificationType.REPLY,
                "회원님의 댓글에 답글이 달렸습니다.", event.boardId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBoardLiked(BoardLikedEvent event) {
        notificationService.create(event.boardOwnerId(), NotificationType.LIKE,
                "회원님의 게시글에 좋아요가 눌렸습니다.", event.boardId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBoardBookmarked(BoardBookmarkedEvent event) {
        notificationService.create(event.boardOwnerId(), NotificationType.BOOKMARK,
                "회원님의 게시글이 북마크(찜)되었습니다.", event.boardId());
    }
}
