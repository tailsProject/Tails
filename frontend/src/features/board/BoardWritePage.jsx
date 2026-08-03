import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  createBoard,
  createDraft,
  updateBoard,
  publishDraft,
  getBoardDetail,
  getImages,
  uploadImages,
  reorderImages,
  deleteImage,
} from './api';
import { useToast } from '../../hooks/useToast';
import Button from '../../components/Button/Button';
import { resolveImage } from '../../utils/resolveImage';
import RichTextEditor from './RichTextEditor';
import styles from './BoardWritePage.module.scss';

// 게시글 작성/수정 페이지, 리치 텍스트 에디터와 이미지 업로드 담당
function escapeHtml(text) {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

// 구버전 PLAIN 글을 처음 수정할 때 리치 텍스트 에디터에 넣을 초기 HTML로 변환
function plainContentToInitialHtml(content, images) {
  const imageHtml = images
    .map((img) => `<img src="${resolveImage(img.imageUrl)}"${img.sequence === 0 ? ' data-cover="true"' : ''}>`)
    .join('');
  const textHtml = (content ?? '')
    .split(/\r\n|\n/)
    .map((line) => `<p>${escapeHtml(line) || '<br>'}</p>`)
    .join('');
  return imageHtml + textHtml;
}

function isContentEmpty(html) {
  const hasText = html.replace(/<[^>]*>/g, '').trim() !== '';
  const hasImage = html.includes('<img');
  return !hasText && !hasImage;
}

// 에디터 HTML에서 이미지 태그만 추출, 대표 이미지 표시 여부 포함
function extractImageEls(html) {
  const doc = new DOMParser().parseFromString(html, 'text/html');
  return Array.from(doc.querySelectorAll('img')).map((el) => ({
    src: el.getAttribute('src'),
    isCover: el.getAttribute('data-cover') === 'true',
  }));
}

export default function BoardWritePage() {
  const { boardId } = useParams();
  const isEdit = Boolean(boardId);
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [status, setStatus] = useState(null);
  const [existingImages, setExistingImages] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [isLoaded, setIsLoaded] = useState(!isEdit);
  const pendingFilesRef = useRef(new Map());

  useEffect(() => {
    if (!isEdit) return;
    Promise.all([getBoardDetail(boardId), getImages(boardId)]).then(([detailRes, imagesRes]) => {
      const detail = detailRes.data.data;
      const images = imagesRes.data.data;
      setTitle(detail.title);
      setStatus(detail.status);
      setExistingImages(images);
      setContent(detail.contentFormat === 'HTML' ? detail.content : plainContentToInitialHtml(detail.content, images));
      setIsLoaded(true);
    });
  }, [boardId, isEdit]);

  useEffect(() => {
    const pending = pendingFilesRef.current;
    return () => {
      pending.forEach((_file, blobUrl) => URL.revokeObjectURL(blobUrl));
    };
  }, []);

  function handleImageFileSelected(file, blobUrl) {
    pendingFilesRef.current.set(blobUrl, file);
  }

  if (!isLoaded) {
    return <div className={styles.wrapper}>불러오는 중...</div>;
  }

  // 에디터에 붙여넣은 로컬(blob) 이미지를 서버에 업로드하고, 삭제된 이미지는 정리하고,
  // 본문에 남은 이미지 순서에 맞춰 서버 순서도 재배치
  async function persistContentAndImages(targetBoardId, html) {
    const pendingSrcs = [...new Set(extractImageEls(html).map((img) => img.src).filter((src) => src?.startsWith('blob:')))];
    const filesToUpload = pendingSrcs.map((src) => pendingFilesRef.current.get(src)).filter(Boolean);

    const urlToImageId = new Map(existingImages.map((img) => [resolveImage(img.imageUrl), img.imageId]));
    let finalHtml = html;

    if (filesToUpload.length > 0) {
      const res = await uploadImages(targetBoardId, filesToUpload);
      res.data.data.forEach((uploaded, i) => {
        const realUrl = resolveImage(uploaded.imageUrl);
        finalHtml = finalHtml.split(pendingSrcs[i]).join(realUrl);
        urlToImageId.set(realUrl, uploaded.imageId);
        URL.revokeObjectURL(pendingSrcs[i]);
        pendingFilesRef.current.delete(pendingSrcs[i]);
      });
    }

    const finalImages = extractImageEls(finalHtml);
    const remainingUrls = new Set(finalImages.map((img) => img.src));
    for (const img of existingImages) {
      const url = resolveImage(img.imageUrl);
      if (!remainingUrls.has(url)) {
        await deleteImage(img.imageId);
        urlToImageId.delete(url);
      }
    }
    const coverIndex = finalImages.findIndex((img) => img.isCover);
    const orderedImages =
      coverIndex > 0 ? [finalImages[coverIndex], ...finalImages.filter((_, i) => i !== coverIndex)] : finalImages;
    const orderedImageIds = orderedImages.map((img) => urlToImageId.get(img.src)).filter(Boolean);
    if (orderedImageIds.length > 0) {
      await reorderImages(targetBoardId, orderedImageIds);
    }

    return finalHtml;
  }

  function buildContentOrWarn() {
    if (isContentEmpty(content)) {
      showToast('내용을 입력해주세요.', 'error');
      return null;
    }
    return content;
  }

  async function handleSaveDraft() {
    const currentContent = buildContentOrWarn();
    if (currentContent === null) return;

    setIsSubmitting(true);
    try {
      let targetBoardId = boardId;
      if (!isEdit) {
        const res = await createDraft({ title, content: currentContent });
        targetBoardId = res.data.data;
      } else {
        await updateBoard(boardId, { title, content: currentContent });
      }
      if (currentContent.includes('<img')) {
        const finalContent = await persistContentAndImages(targetBoardId, currentContent);
        await updateBoard(targetBoardId, { title, content: finalContent });
      }
      showToast('임시저장되었습니다.', 'success');
      navigate('/boards/drafts', { replace: true });
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '임시저장에 실패했습니다.', 'error');
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handlePublish(e) {
    e.preventDefault();
    const currentContent = buildContentOrWarn();
    if (currentContent === null) return;

    setIsSubmitting(true);
    try {
      let targetBoardId = boardId;
      if (!isEdit) {
        const res = await createBoard({ title, content: currentContent });
        targetBoardId = res.data.data;
      } else if (status === 'DRAFT') {
        await publishDraft(boardId, { title, content: currentContent });
      } else {
        await updateBoard(boardId, { title, content: currentContent });
      }
      if (currentContent.includes('<img')) {
        const finalContent = await persistContentAndImages(targetBoardId, currentContent);
        await updateBoard(targetBoardId, { title, content: finalContent });
      }
      navigate(`/boards/${targetBoardId}`, { replace: true });
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '저장에 실패했습니다.', 'error');
    } finally {
      setIsSubmitting(false);
    }
  }

  const canSaveDraft = !isEdit || status === 'DRAFT';

  return (
    <div className={styles.wrapper}>
      <h1>{isEdit ? '게시글 수정' : '게시글 작성'}</h1>
      <form onSubmit={handlePublish} className={styles.form}>
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

        <RichTextEditor
          value={content}
          onChange={setContent}
          onImageFileSelected={handleImageFileSelected}
          placeholder="내용을 입력하세요"
        />

        <div className={styles.buttonRow}>
          {canSaveDraft && (
            <Button type="button" variant="secondary" disabled={isSubmitting} onClick={handleSaveDraft}>
              임시저장
            </Button>
          )}
          <Button type="submit" disabled={isSubmitting}>
            {isEdit && status !== 'DRAFT' ? '수정 완료' : '발행하기'}
          </Button>
        </div>
      </form>
    </div>
  );
}
