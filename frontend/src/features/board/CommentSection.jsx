// 게시글 댓글/대댓글 목록, 작성 담당
import { useEffect, useRef, useState } from 'react';
import { getComments, createComment, updateComment, deleteComment } from './api';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { useConfirm } from '../../hooks/useConfirm';
import Button from '../../components/Button/Button';
import Pagination from '../../components/Pagination/Pagination';
import { resolveImage } from '../../utils/resolveImage';
import styles from './CommentSection.module.scss';

const DELETED_TEXT = '삭제된 댓글입니다.';

export default function CommentSection({ boardId, boardAuthorId }) {
  const { member, isAuthenticated } = useAuth();
  const { showToast } = useToast();
  const confirm = useConfirm();
  const [commentPage, setCommentPage] = useState(null);
  const [page, setPage] = useState(0);
  const [content, setContent] = useState('');
  const [replyTarget, setReplyTarget] = useState(null);
  const [replyContent, setReplyContent] = useState('');
  const [editTarget, setEditTarget] = useState(null);
  const [editContent, setEditContent] = useState('');
  const commentTextareaRef = useRef(null);

  function autoResizeTextarea(e) {
    e.target.style.height = 'auto';
    e.target.style.height = `${e.target.scrollHeight}px`;
  }

  async function load() {
    const res = await getComments(boardId, { page });
    setCommentPage(res.data.data);
  }

  useEffect(() => {
    load();
  }, [boardId, page]);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!isAuthenticated) {
      showToast('로그인이 필요합니다.', 'error');
      return;
    }
    if (!content.trim()) return;
    await createComment(boardId, { content });
    setContent('');
    if (commentTextareaRef.current) {
      commentTextareaRef.current.style.height = 'auto';
    }
    load();
  }

  async function handleReplySubmit(parentId) {
    if (!replyContent.trim()) return;
    await createComment(boardId, { content: replyContent, parentId });
    setReplyContent('');
    setReplyTarget(null);
    load();
  }

  async function handleEditSubmit(commentId) {
    if (!editContent.trim()) return;
    await updateComment(boardId, commentId, editContent);
    setEditTarget(null);
    load();
  }

  async function handleDelete(commentId) {
    const ok = await confirm('댓글을 삭제하시겠습니까?');
    if (!ok) return;
    await deleteComment(boardId, commentId);
    load();
  }

  function handleReplyClick(commentId) {
    if (!isAuthenticated) {
      showToast('로그인이 필요합니다.', 'error');
      return;
    }
    setReplyTarget(replyTarget === commentId ? null : commentId);
  }

  // 답글의 답글까지 화면에서는 한 단계로 평평하게 펼쳐서 표시, 대상이 다르면 멘션 표시
  function flattenReplies(replies, rootAuthorNickname, parentAuthorNickname) {
    const flat = [];
    for (const reply of replies) {
      flat.push({ ...reply, replyToNickname: parentAuthorNickname !== rootAuthorNickname ? parentAuthorNickname : null });
      if (reply.replies?.length > 0) {
        flat.push(...flattenReplies(reply.replies, rootAuthorNickname, reply.authorNickname));
      }
    }
    return flat;
  }

  function renderCommentBody(comment, isDeleted, isOwner, isAdmin) {
    return (
      <>
        <div className={styles.commentHeader}>
          <span className={styles.authorAvatar}>
            {resolveImage(comment.authorProfileImg) ? (
              <img src={resolveImage(comment.authorProfileImg)} alt="" />
            ) : (
              comment.authorNickname.slice(0, 1)
            )}
          </span>
          <span className={styles.author}>{comment.authorNickname}</span>
          {comment.authorId != null && comment.authorId === boardAuthorId && (
            <span className={styles.authorBadge}>작성자</span>
          )}
          <span className={styles.date}>{new Date(comment.createdAt).toLocaleString()}</span>
        </div>

        {editTarget === comment.commentId ? (
          <div className={styles.editForm}>
            <input value={editContent} onChange={(e) => setEditContent(e.target.value)} />
            <Button variant="text" onClick={() => handleEditSubmit(comment.commentId)}>
              저장
            </Button>
            <Button variant="text" onClick={() => setEditTarget(null)}>
              취소
            </Button>
          </div>
        ) : (
          <p className={isDeleted ? styles.deletedContent : styles.content}>
            {comment.replyToNickname && <strong className={styles.mention}>@{comment.replyToNickname} </strong>}
            {comment.content}
          </p>
        )}

        <div className={styles.commentActions}>
          {!isDeleted && (
            <button onClick={() => handleReplyClick(comment.commentId)}>
              답글
            </button>
          )}
          {isOwner && !isDeleted && editTarget !== comment.commentId && (
            <button
              onClick={() => {
                setEditTarget(comment.commentId);
                setEditContent(comment.content);
              }}
            >
              수정
            </button>
          )}
          {(isOwner || isAdmin) && !isDeleted && editTarget !== comment.commentId && (
            <button onClick={() => handleDelete(comment.commentId)}>
              {isOwner ? '삭제' : '삭제 (관리자)'}
            </button>
          )}
        </div>

        {replyTarget === comment.commentId && (
          <div className={styles.replyForm}>
            <textarea
              value={replyContent}
              onChange={(e) => {
                setReplyContent(e.target.value);
                autoResizeTextarea(e);
              }}
              placeholder={`${comment.authorNickname}님에게 답글`}
              rows={1}
              spellCheck={false}
            />
            <Button variant="text" onClick={() => handleReplySubmit(comment.commentId)}>
              등록
            </Button>
          </div>
        )}
      </>
    );
  }

  function renderComment(comment) {
    const isOwner = isAuthenticated && member.memberId === comment.authorId;
    const isAdmin = isAuthenticated && member.role === 'ADMIN';
    const isDeleted = comment.content === DELETED_TEXT;
    const flatReplies = flattenReplies(comment.replies ?? [], comment.authorNickname, comment.authorNickname);

    return (
      <li key={comment.commentId} className={styles.comment}>
        {renderCommentBody(comment, isDeleted, isOwner, isAdmin)}

        {flatReplies.length > 0 && (
          <ul className={styles.replies}>
            {flatReplies.map((reply) => {
              const replyIsOwner = isAuthenticated && member.memberId === reply.authorId;
              const replyIsAdmin = isAuthenticated && member.role === 'ADMIN';
              const replyIsDeleted = reply.content === DELETED_TEXT;
              return (
                <li key={reply.commentId} className={styles.reply}>
                  {renderCommentBody(reply, replyIsDeleted, replyIsOwner, replyIsAdmin)}
                </li>
              );
            })}
          </ul>
        )}
      </li>
    );
  }

  return (
    <div className={styles.wrapper}>
      <h2>댓글 {commentPage?.totalElements ?? 0}</h2>

      <form onSubmit={handleSubmit} className={styles.form}>
        <textarea
          ref={commentTextareaRef}
          value={content}
          onChange={(e) => {
            setContent(e.target.value);
            autoResizeTextarea(e);
          }}
          placeholder={isAuthenticated ? '댓글을 입력하세요' : '로그인 후 댓글을 작성할 수 있습니다'}
          disabled={!isAuthenticated}
          rows={1}
          spellCheck={false}
        />
        <Button type="submit" disabled={!isAuthenticated}>
          등록
        </Button>
      </form>

      {commentPage && (
        <>
          <ul className={styles.list}>{commentPage.content.map((comment) => renderComment(comment))}</ul>
          <Pagination page={commentPage.number} totalPages={commentPage.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
