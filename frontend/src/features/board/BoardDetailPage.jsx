// 게시글 상세 - 신고 버튼을 붙일 최소 뼈대 화면(박영준의 Board 피드가 아직 없어서 임시로 구현).
// 박영준 쪽 실제 게시글 상세 화면이 나오면 신고 버튼만 옮겨 붙이면 됨
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getBoardDetail, getComments } from './api';
import ReportModal from '../report/ReportModal';
import StateMessage from '../../components/StateMessage/StateMessage';
import { WarningIcon, SuitcaseIcon } from '../../components/Icon/Icon';
import styles from './BoardDetailPage.module.scss';

export default function BoardDetailPage() {
  const { boardId } = useParams();
  const [board, setBoard] = useState(null);
  const [comments, setComments] = useState([]);
  const [notFound, setNotFound] = useState(false);
  const [reportTarget, setReportTarget] = useState(null); // { targetType, targetId }

  useEffect(() => {
    getBoardDetail(boardId)
      .then((res) => setBoard(res.data.data))
      .catch(() => setNotFound(true));
    getComments(boardId).then((res) => setComments(res.data.data.content));
  }, [boardId]);

  if (notFound) {
    return (
      <StateMessage
        icon={SuitcaseIcon}
        title="게시글을 찾을 수 없어요"
        description="삭제되었거나 존재하지 않는 게시글이에요."
        actionTo="/"
        actionLabel="홈으로"
      />
    );
  }

  if (!board) {
    return null;
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.header}>
        <h1>{board.title}</h1>
        <button
          type="button"
          className={styles.reportBtn}
          onClick={() => setReportTarget({ targetType: 'BOARD', targetId: board.boardId })}
          aria-label="게시글 신고"
        >
          <WarningIcon /> 신고
        </button>
      </div>
      <p className={styles.author}>{board.authorNickname}</p>
      <p className={styles.content}>{board.content}</p>

      <h2 className={styles.commentsTitle}>댓글 {comments.length}개</h2>
      <ul className={styles.commentList}>
        {comments.map((comment) => (
          <li key={comment.commentId} className={styles.comment}>
            <div className={styles.commentBody}>
              <p className={styles.commentAuthor}>{comment.authorNickname}</p>
              <p className={styles.commentContent}>{comment.content}</p>
            </div>
            <button
              type="button"
              className={styles.reportBtn}
              onClick={() => setReportTarget({ targetType: 'COMMENT', targetId: comment.commentId })}
              aria-label="댓글 신고"
            >
              <WarningIcon />
            </button>
          </li>
        ))}
      </ul>

      <ReportModal
        open={Boolean(reportTarget)}
        onClose={() => setReportTarget(null)}
        targetType={reportTarget?.targetType}
        targetId={reportTarget?.targetId}
      />
    </div>
  );
}
