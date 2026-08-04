// 장소 리뷰 목록, 작성, 별점, 이미지 첨부 담당
import { useEffect, useState } from 'react';
import {
  getReviews,
  createReview,
  updateReview,
  deleteReview,
  uploadReviewImages,
  getReviewImages,
  deleteReviewImage,
} from './api';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { useConfirm } from '../../hooks/useConfirm';
import Button from '../../components/Button/Button';
import Pagination from '../../components/Pagination/Pagination';
import { resolveImage } from '../../utils/resolveImage';
import ReportModal from '../report/ReportModal';
import { WarningIcon } from '../../components/Icon/Icon';
import styles from './ReviewSection.module.scss';

const STARS = [1, 2, 3, 4, 5];

function StarPicker({ value, onChange }) {
  return (
    <div className={styles.starPicker}>
      {STARS.map((star) => (
        <button
          type="button"
          key={star}
          onClick={() => onChange(star)}
          className={star <= value ? styles.starActive : styles.star}
        >
          ★
        </button>
      ))}
    </div>
  );
}

export default function ReviewSection({ placeId }) {
  const { member, isAuthenticated } = useAuth();
  const { showToast } = useToast();
  const confirm = useConfirm();

  const [page, setPage] = useState(0);
  // reviews 페이지, averageRating, reviewCount 포함
  const [reviewData, setReviewData] = useState(null);
  const [imagesByReview, setImagesByReview] = useState({});
  const [rating, setRating] = useState(0);
  const [content, setContent] = useState('');
  const [files, setFiles] = useState([]);
  const [editingId, setEditingId] = useState(null);
  const [editRating, setEditRating] = useState(0);
  const [editContent, setEditContent] = useState('');
  const [reportTargetId, setReportTargetId] = useState(null);

  async function load() {
    const res = await getReviews(placeId, { page });
    setReviewData(res.data.data);
    const entries = await Promise.all(
      res.data.data.reviews.content.map(async (review) => {
        const imgRes = await getReviewImages(review.reviewId);
        return [review.reviewId, imgRes.data.data];
      }),
    );
    setImagesByReview(Object.fromEntries(entries));
  }

  useEffect(() => {
    load();
  }, [placeId, page, isAuthenticated]);

  function handleFileSelect(e) {
    const selected = Array.from(e.target.files);
    setFiles((prev) => [...prev, ...selected]);
    e.target.value = '';
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!isAuthenticated) {
      showToast('로그인이 필요합니다.', 'error');
      return;
    }
    if (rating === 0) {
      showToast('별점을 선택해주세요.', 'error');
      return;
    }
    try {
      const res = await createReview(placeId, { rating, content });
      if (files.length > 0) {
        await uploadReviewImages(res.data.data, files);
      }
      setRating(0);
      setContent('');
      setFiles([]);
      load();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '리뷰 작성에 실패했습니다.', 'error');
    }
  }

  function startEdit(review) {
    setEditingId(review.reviewId);
    setEditRating(review.rating);
    setEditContent(review.content);
  }

  async function handleEditSubmit(reviewId) {
    try {
      await updateReview(placeId, reviewId, { rating: editRating, content: editContent });
      setEditingId(null);
      load();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '수정에 실패했습니다.', 'error');
    }
  }

  async function handleDelete(reviewId) {
    const ok = await confirm('리뷰를 삭제하시겠습니까?');
    if (!ok) return;
    await deleteReview(placeId, reviewId);
    load();
  }

  async function handleDeleteImage(reviewId, imageId) {
    const ok = await confirm('이 사진을 삭제하시겠습니까?');
    if (!ok) return;
    await deleteReviewImage(imageId);
    setImagesByReview((prev) => ({
      ...prev,
      [reviewId]: prev[reviewId].filter((img) => img.imageId !== imageId),
    }));
  }

  if (!reviewData) {
    return null;
  }

  return (
    <div className={styles.wrapper}>
      <h2>
        리뷰 {reviewData.reviewCount} · 평균 {reviewData.averageRating.toFixed(1)}★
      </h2>

      <form onSubmit={handleSubmit} className={styles.form}>
        <StarPicker value={rating} onChange={setRating} />
        <textarea
          placeholder={isAuthenticated ? '리뷰를 입력하세요' : '로그인 후 리뷰를 작성할 수 있습니다'}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          disabled={!isAuthenticated}
          rows={3}
        />
        {files.length > 0 && (
          <ul className={styles.newFileList}>
            {files.map((file, index) => (
              <li key={`${file.name}-${index}`}>{file.name}</li>
            ))}
          </ul>
        )}
        <div className={styles.formFooter}>
          <input
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            multiple
            disabled={!isAuthenticated}
            onChange={handleFileSelect}
          />
          <Button type="submit" disabled={!isAuthenticated}>
            등록
          </Button>
        </div>
      </form>

      <ul className={styles.list}>
        {reviewData.reviews.content.map((review) => {
          const isOwner = isAuthenticated && member.memberId === review.authorId;
          const isAdmin = isAuthenticated && member.role === 'ADMIN';
          const images = imagesByReview[review.reviewId] ?? [];
          return (
            <li key={review.reviewId} className={styles.item}>
              <div className={styles.itemHeader}>
                <span className={styles.author}>{review.authorNickname}</span>
                <span className={styles.rating}>{'★'.repeat(review.rating)}</span>
                <span className={styles.date}>{new Date(review.createdAt).toLocaleDateString()}</span>
                <button
                  type="button"
                  className={styles.reportBtn}
                  onClick={() => setReportTargetId(review.reviewId)}
                  aria-label="리뷰 신고"
                >
                  <WarningIcon />
                </button>
              </div>

              {editingId === review.reviewId ? (
                <div className={styles.editForm}>
                  <StarPicker value={editRating} onChange={setEditRating} />
                  <textarea value={editContent} onChange={(e) => setEditContent(e.target.value)} rows={3} />
                  <div>
                    <Button variant="text" onClick={() => handleEditSubmit(review.reviewId)}>
                      저장
                    </Button>
                    <Button variant="text" onClick={() => setEditingId(null)}>
                      취소
                    </Button>
                  </div>
                </div>
              ) : (
                <p className={styles.content}>{review.content}</p>
              )}

              {images.length > 0 && (
                <div className={styles.images}>
                  {images.map((img) => (
                    <div key={img.imageId} className={styles.imageItem}>
                      <img src={resolveImage(img.imageUrl)} alt="" />
                      {isOwner && (
                        <button
                          type="button"
                          className={styles.imageRemoveButton}
                          onClick={() => handleDeleteImage(review.reviewId, img.imageId)}
                          aria-label="사진 삭제"
                        >
                          ✕
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              )}

              {(isOwner || isAdmin) && editingId !== review.reviewId && (
                <div className={styles.actions}>
                  {isOwner && <button onClick={() => startEdit(review)}>수정</button>}
                  <button onClick={() => handleDelete(review.reviewId)}>
                    {isOwner ? '삭제' : '삭제 (관리자)'}
                  </button>
                </div>
              )}
            </li>
          );
        })}
        {reviewData.reviews.content.length === 0 && <p className={styles.empty}>작성된 리뷰가 없습니다.</p>}
      </ul>

      <Pagination
        page={reviewData.reviews.number}
        totalPages={reviewData.reviews.totalPages}
        onPageChange={setPage}
      />

      <ReportModal
        open={reportTargetId != null}
        onClose={() => setReportTargetId(null)}
        targetType="REVIEW"
        targetId={reportTargetId}
      />
    </div>
  );
}
