// 내가 쓴 장소 리뷰 목록 페이지
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyReviews } from './api';
import Pagination from '../../components/Pagination/Pagination';
import { StarIcon } from '../../components/Icon/Icon';
import styles from './ListPage.module.scss';

export default function MyReviewsPage() {
  const [page, setPage] = useState(0);
  const [reviewPage, setReviewPage] = useState(null);

  useEffect(() => {
    getMyReviews({ page }).then((res) => setReviewPage(res.data.data));
  }, [page]);

  return (
    <div>
      <div className={styles.header}>
        <h1>내가 쓴 리뷰</h1>
      </div>

      {reviewPage && (
        <>
          <ul className={styles.list}>
            {reviewPage.content.map((review) => (
              <li key={review.reviewId}>
                <Link to={`/places/${review.placeId}`} className={styles.item}>
                  <span className={styles.itemThumb}><StarIcon /></span>
                  <span className={styles.itemBody}>
                    <span className={styles.title}>{review.placeName}</span>
                    <span className={styles.starRow}>
                      {[1, 2, 3, 4, 5].map((i) => (
                        <StarIcon
                          key={i}
                          fill={i <= review.rating ? 'currentColor' : 'none'}
                          className={i <= review.rating ? styles.starFilled : styles.starEmpty}
                        />
                      ))}
                    </span>
                    <span className={styles.excerpt}>{review.content}</span>
                    <span className={styles.metaDate}>{new Date(review.createdAt).toLocaleDateString()}</span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
          {reviewPage.content.length === 0 && (
            <div className={styles.empty}>
              <span className={styles.emptyIcon}><StarIcon /></span>
              <p>작성한 리뷰가 없습니다.</p>
            </div>
          )}
          <Pagination page={reviewPage.number} totalPages={reviewPage.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
