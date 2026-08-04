// 내가 신고한 내역 목록 페이지
import { useEffect, useState } from 'react';
import { getMyReports } from './api';
import Pagination from '../../components/Pagination/Pagination';
import { WarningIcon, PencilIcon, ChatBubbleIcon, UserIcon } from '../../components/Icon/Icon';
import styles from './ListPage.module.scss';

const TARGET_TYPE_LABEL = { BOARD: '게시글', COMMENT: '댓글', MEMBER: '회원' };
const TARGET_TYPE_ICON = { BOARD: PencilIcon, COMMENT: ChatBubbleIcon, MEMBER: UserIcon };
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
            {reportPage.content.map((report) => {
              const TargetIcon = TARGET_TYPE_ICON[report.targetType] ?? WarningIcon;
              return (
                <li key={report.reportId} className={styles.item}>
                  <span className={styles.itemThumb}><TargetIcon /></span>
                  <span className={styles.itemBody}>
                    <span className={styles.title}>{TARGET_TYPE_LABEL[report.targetType]} 신고</span>
                    <span className={styles.excerpt}>{report.reason}</span>
                    <span className={styles.metaRow}>
                      <span
                        className={`${styles.badge} ${
                          report.status === 'PENDING' ? styles.badgePending : styles.badgeResolved
                        }`}
                      >
                        {STATUS_LABEL[report.status]}
                      </span>
                      <span className={styles.metaDate}>{new Date(report.createdAt).toLocaleDateString()}</span>
                    </span>
                  </span>
                </li>
              );
            })}
          </ul>
          {reportPage.content.length === 0 && (
            <div className={styles.empty}>
              <span className={styles.emptyIcon}><WarningIcon /></span>
              <p>신고한 내역이 없습니다.</p>
            </div>
          )}
          <Pagination page={reportPage.number} totalPages={reportPage.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
