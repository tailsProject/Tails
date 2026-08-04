// 게시글 상세 페이지, 리치 텍스트/구버전 텍스트 글 모두 처리
import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import DOMPurify from 'dompurify';
import { getBoardDetail, deleteBoard, toggleLike, toggleBookmark, getImages } from './api';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { useConfirm } from '../../hooks/useConfirm';
import Button from '../../components/Button/Button';
import CommentSection from './CommentSection';
import ReportModal from '../report/ReportModal';
import StateMessage from '../../components/StateMessage/StateMessage';
import { resolveImage } from '../../utils/resolveImage';
import { HeartIcon, BookmarkIcon, PencilIcon } from '../../components/Icon/Icon';
import { IMAGE_MARKER_PATTERN } from './contentBlocks';
import styles from './BoardDetailPage.module.scss';

// 구버전 텍스트 글의 [[img:N]] 마커를 실제 이미지 태그로 치환
function renderInlineContent(content, images) {
  const imagesBySequence = new Map(images.map((img) => [img.sequence, img]));
  const parts = content.split(IMAGE_MARKER_PATTERN);
  return parts.map((part, index) => {
    if (index % 2 === 1) {
      const image = imagesBySequence.get(Number(part));
      return image ? (
        <img
          key={`img-${image.imageId}`}
          className={styles.inlineImage}
          src={resolveImage(image.imageUrl)}
          alt=""
        />
      ) : null;
    }
    return part;
  });
}

export default function BoardDetailPage() {
  const { boardId } = useParams();
  const navigate = useNavigate();
  const { member, isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const { showToast } = useToast();
  const confirm = useConfirm();
  const [board, setBoard] = useState(null);
  const [images, setImages] = useState([]);
  const [reportOpen, setReportOpen] = useState(false);
  const [notFound, setNotFound] = useState(false);

  // StrictMode 이중 렌더링에도 조회수가 두 번 오르지 않도록 방지
  const requestedBoardIdRef = useRef(null);

  useEffect(() => {
    if (isAuthLoading) {
      return;
    }
    if (requestedBoardIdRef.current === boardId) {
      return;
    }
    requestedBoardIdRef.current = boardId;
    getBoardDetail(boardId)
      .then((res) => setBoard(res.data.data))
      .catch(() => setNotFound(true));
    getImages(boardId).then((res) => setImages(res.data.data));
  }, [boardId, isAuthLoading]);

  async function handleLike() {
    if (!isAuthenticated) {
      showToast('로그인이 필요합니다.', 'error');
      return;
    }
    try {
      const res = await toggleLike(boardId);
      setBoard((prev) => ({ ...prev, liked: res.data.data.liked, likeCount: res.data.data.likeCount }));
    } catch {
      showToast('잠시 후 다시 시도해주세요.', 'error');
    }
  }

  async function handleBookmark() {
    if (!isAuthenticated) {
      showToast('로그인이 필요합니다.', 'error');
      return;
    }
    const res = await toggleBookmark(boardId);
    setBoard((prev) => ({ ...prev, bookmarked: res.data.data.bookmarked }));
  }

  async function handleDelete() {
    const ok = await confirm('정말 삭제하시겠습니까?\n삭제한 게시글은 복구할 수 없습니다.');
    if (!ok) return;
    await deleteBoard(boardId);
    showToast('삭제되었습니다.', 'success');
    navigate('/boards');
  }

  if (notFound) {
    return (
      <StateMessage
        icon={PencilIcon}
        title="게시글을 찾을 수 없어요"
        description="삭제되었거나 존재하지 않는 게시글이에요."
        actionTo="/boards"
        actionLabel="목록으로"
      />
    );
  }

  if (!board) {
    return null;
  }

  const isOwner = isAuthenticated && member.memberId === board.authorId;
  const isAdmin = isAuthenticated && member.role === 'ADMIN';
  const isRichText = board.contentFormat === 'HTML';
  const hasInlineImages = !isRichText && IMAGE_MARKER_PATTERN.test(board.content ?? '');

  return (
    <div className={styles.wrapper}>
      <h1>{board.title}</h1>
      <div className={styles.meta}>
        <span className={styles.authorAvatar}>
          {resolveImage(board.authorProfileImg) ? (
            <img src={resolveImage(board.authorProfileImg)} alt="" />
          ) : (
            board.authorNickname.slice(0, 1)
          )}
        </span>
        <span>{board.authorNickname}</span>
        <span>조회 {board.viewCount}</span>
        <span>{new Date(board.createdAt).toLocaleString()}</span>
      </div>

      {isRichText ? (
        <div
          className={`${styles.content} ${styles.richContent}`}
          dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(board.content ?? '') }}
        />
      ) : hasInlineImages ? (
        <div className={styles.content}>{renderInlineContent(board.content, images)}</div>
      ) : (
        <>
          {images.length > 0 && (
            <div className={styles.images}>
              {images.map((img) => (
                <img key={img.imageId} src={resolveImage(img.imageUrl)} alt="" />
              ))}
            </div>
          )}
          <p className={styles.content}>{board.content}</p>
        </>
      )}

      <div className={styles.actions}>
        <button onClick={handleLike} className={board.liked ? styles.active : ''}>
          <HeartIcon fill={board.liked ? 'currentColor' : 'none'} /> {board.likeCount}
        </button>
        <button onClick={handleBookmark} className={board.bookmarked ? styles.active : ''}>
          <BookmarkIcon fill={board.bookmarked ? 'currentColor' : 'none'} />
        </button>
        <div className={styles.spacer} />
        {isOwner && (
          <Link to={`/boards/${boardId}/edit`}>
            <Button variant="secondary">수정</Button>
          </Link>
        )}
        {(isOwner || isAdmin) && (
          <Button variant="secondary" onClick={handleDelete}>
            {isOwner ? '삭제' : '삭제 (관리자)'}
          </Button>
        )}
        {!isOwner && isAuthenticated && (
          <Button variant="text" onClick={() => setReportOpen(true)}>
            신고
          </Button>
        )}
      </div>

      <CommentSection boardId={boardId} boardAuthorId={board.authorId} />

      <ReportModal
        open={reportOpen}
        onClose={() => setReportOpen(false)}
        targetType="BOARD"
        targetId={Number(boardId)}
      />
    </div>
  );
}
