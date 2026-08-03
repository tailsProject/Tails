import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyReviews } from './api';
import Pagination from '../../components/Pagination/Pagination';
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
                  <span className={styles.title}>
                    {review.placeName} {'★'.repeat(review.rating)}
                  </span>
                  <span className={styles.meta}>{review.content}</span>
                  <span className={styles.meta}>{new Date(review.createdAt).toLocaleDateString()}</span>
                </Link>
              </li>
            ))}
            {reviewPage.content.length === 0 && <p className={styles.empty}>작성한 리뷰가 없습니다.</p>}
          </ul>
          <Pagination page={reviewPage.number} totalPages={reviewPage.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
