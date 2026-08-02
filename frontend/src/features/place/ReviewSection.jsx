// 장소 리뷰 목록 + 신고 버튼 - Place 지도(박영준 트랙)가 아직 없어서 최소 뼈대로 구현.
// 나중에 실제 장소 상세 화면에 그대로 옮겨 붙이면 됨
import { useEffect, useState } from 'react';
import { getPlaceReviews } from './api';
import ReportModal from '../report/ReportModal';
import { StarIcon, WarningIcon } from '../../components/Icon/Icon';
import styles from './ReviewSection.module.scss';

export default function ReviewSection({ placeId }) {
  const [reviewData, setReviewData] = useState(null);
  const [reportTargetId, setReportTargetId] = useState(null);

  useEffect(() => {
    getPlaceReviews(placeId).then((res) => setReviewData(res.data.data));
  }, [placeId]);

  if (!reviewData) {
    return null;
  }

  return (
    <div className={styles.wrapper}>
      <h2 className={styles.title}>
        <StarIcon /> 리뷰 {reviewData.reviewCount}개 · 평균 {reviewData.averageRating.toFixed(1)}
      </h2>
      <ul className={styles.list}>
        {reviewData.reviews.content.map((review) => (
          <li key={review.reviewId} className={styles.item}>
            <div className={styles.itemBody}>
              <p className={styles.author}>
                {review.authorNickname} · {'★'.repeat(review.rating)}
              </p>
              <p className={styles.content}>{review.content}</p>
            </div>
            <button
              type="button"
              className={styles.reportBtn}
              onClick={() => setReportTargetId(review.reviewId)}
              aria-label="리뷰 신고"
            >
              <WarningIcon />
            </button>
          </li>
        ))}
        {reviewData.reviews.content.length === 0 && <p className={styles.empty}>아직 등록된 리뷰가 없습니다.</p>}
      </ul>

      <ReportModal
        open={reportTargetId != null}
        onClose={() => setReportTargetId(null)}
        targetType="REVIEW"
        targetId={reportTargetId}
      />
    </div>
  );
}
