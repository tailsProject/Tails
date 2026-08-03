import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { createBoard, updateBoard, getBoardDetail } from './api';
import { useToast } from '../../hooks/useToast';
import Button from '../../components/Button/Button';
import styles from './BoardWritePage.module.scss';

// 게시글 작성/수정 페이지 최초 구현, 우선 텍스트만 지원
export default function BoardWritePage() {
  const { boardId } = useParams();
  const isEdit = Boolean(boardId);
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoaded, setIsLoaded] = useState(!isEdit);

  useEffect(() => {
    if (!isEdit) return;
    getBoardDetail(boardId).then((res) => {
      const detail = res.data.data;
      setTitle(detail.title);
      setContent(detail.content);
      setIsLoaded(true);
    });
  }, [boardId, isEdit]);

  if (!isLoaded) {
    return <div className={styles.wrapper}>불러오는 중...</div>;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!content.trim()) {
      showToast('내용을 입력해주세요.', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      if (!isEdit) {
        const res = await createBoard({ title, content });
        navigate(`/boards/${res.data.data}`, { replace: true });
      } else {
        await updateBoard(boardId, { title, content });
        navigate(`/boards/${boardId}`, { replace: true });
      }
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '저장에 실패했습니다.', 'error');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className={styles.wrapper}>
      <h1>{isEdit ? '게시글 수정' : '게시글 작성'}</h1>
      <form onSubmit={handleSubmit} className={styles.form}>
        <input
          type="text"
          placeholder="제목"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') e.preventDefault();
          }}
          maxLength={200}
          required
        />

        <textarea
          placeholder="내용을 입력하세요"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={12}
        />

        <div className={styles.buttonRow}>
          <Button type="submit" disabled={isSubmitting}>
            {isEdit ? '수정 완료' : '발행하기'}
          </Button>
        </div>
      </form>
    </div>
  );
}
