import { useEffect, useState } from 'react';
import { getMyReports } from './api';
import Pagination from '../../components/Pagination/Pagination';
import styles from './ListPage.module.scss';

const TARGET_TYPE_LABEL = { BOARD: '게시글', COMMENT: '댓글', MEMBER: '회원', REVIEW: '리뷰' };
const STATUS_LABEL = { PENDING: '처리 대기', RESOLVED: '처리 완료' };

export default function MyReportsPage() {
  const [page, setPage] = useState(0);
  const [reportPage, setReportPage] = useState(null);

  useEffect(() => {
    getMyReports({ page }).then((res) => setReportPage(res.data.data));
  }, [page]);

  return (
    <div>
      <div className={styles.header}>
        <h1>내 신고 내역</h1>
      </div>

      {reportPage && (
        <>
          <ul className={styles.list}>
            {reportPage.content.map((report) => (
              <li key={report.reportId} className={styles.item}>
                <span className={styles.title}>
                  <span
                    className={`${styles.badge} ${
                      report.status === 'PENDING' ? styles.badgePending : styles.badgeResolved
                    }`}
                  >
                    {STATUS_LABEL[report.status]}
                  </span>
                  {TARGET_TYPE_LABEL[report.targetType]} 신고
                </span>
                <span className={styles.meta}>{report.reason}</span>
                <span className={styles.meta}>{new Date(report.createdAt).toLocaleDateString()}</span>
              </li>
            ))}
            {reportPage.content.length === 0 && <p className={styles.empty}>신고한 내역이 없습니다.</p>}
          </ul>
          <Pagination page={reportPage.number} totalPages={reportPage.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
