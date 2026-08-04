package com.tails.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 댓글 좋아요 조회 레포지토리
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    // 좋아요 여부 확인과 취소 처리용 조회
    Optional<CommentLike> findByCommentIdAndMemberId(Long commentId, Long memberId);

    // 게시글 하나에 대한 회원의 좋아요 기록을 한 번에 조회, 댓글 목록의 좋아요 표시용
    List<CommentLike> findByMemberIdAndComment_Board_Id(Long memberId, Long boardId);
}
