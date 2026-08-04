// 관리자 신고 처리 큐 페이지, 대상 미리보기 포함
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getReportsByStatus, resolveReport, deleteReport } from './api';
import { useToast } from '../../hooks/useToast';
import { useConfirm } from '../../hooks/useConfirm';
import Pagination from '../../components/Pagination/Pagination';
import StateMessage from '../../components/StateMessage/StateMessage';
import { WarningIcon, CheckIcon, TrashIcon } from '../../components/Icon/Icon';
import styles from './AdminReportsPage.module.scss';

const TARGET_TYPE_LABEL = { BOARD: '게시글', COMMENT: '댓글', MEMBER: '회원', REVIEW: '리뷰' };
const TARGET_TYPE_BADGE = {
  BOARD: styles.typeBoard,
  COMMENT: styles.typeComment,
  MEMBER: styles.typeMember,
  REVIEW: styles.typeReview,
};

export default function AdminReportsPage() {
  const { showToast } = useToast();
  const confirm = useConfirm();
  const [status, setStatus] = useState('PENDING');
  const [page, setPage] = useState(0);
  const [reportPage, setReportPage] = useState(null);
  const [accessDenied, setAccessDenied] = useState(false);

  async function load() {
    try {
      const res = await getReportsByStatus({ status, page });
      setReportPage(res.data.data);
    } catch (error) {
      if (error.response?.status === 403) {
        setAccessDenied(true);
      } else {
        showToast('신고 목록을 불러오지 못했습니다.', 'error');
      }
    }
  }

  useEffect(() => {
    load();
  }, [status, page]);

  async function handleResolve(reportId) {
    await resolveReport(reportId);
    showToast('처리 완료로 표시했습니다.', 'success');
    load();
  }

  async function handleDelete(reportId) {
    const ok = await confirm('이 신고 기록을 삭제하시겠습니까?\n신고 대상 게시글/댓글은 삭제되지 않습니다.');
    if (!ok) return;
    await deleteReport(reportId);
    showToast('신고 기록을 삭제했습니다.', 'success');
    load();
  }

  if (accessDenied) {
    return (
      <StateMessage
        icon={WarningIcon}
        title="접근 권한이 없어요"
        description="관리자만 볼 수 있는 페이지예요."
        actionTo="/mypage"
        actionLabel="마이페이지로"
      />
    );
  }

  return (
    <div>
      <div className={styles.header}>
        <h1>신고 처리</h1>
        {reportPage && <span className={styles.count}>전체 {reportPage.totalElements.toLocaleString()}건</span>}
      </div>

      <div className={styles.tabs}>
        <button
          className={status === 'PENDING' ? styles.tabActive : ''}
          onClick={() => {
            setStatus('PENDING');
            setPage(0);
          }}
        >
          처리 대기
        </button>
        <button
          className={status === 'RESOLVED' ? styles.tabActive : ''}
          onClick={() => {
            setStatus('RESOLVED');
            setPage(0);
          }}
        >
          처리 완료
        </button>
      </div>

      {reportPage && (
        <>
          <ul className={styles.list}>
            {reportPage.content.map((report) => (
              <li key={report.reportId} className={styles.item}>
                <div className={styles.itemTop}>
                  <span className={`${styles.typeBadge} ${TARGET_TYPE_BADGE[report.targetType] ?? ''}`}>
                    {TARGET_TYPE_LABEL[report.targetType]} #{report.targetId}
                  </span>
                  <span className={styles.reporter}>{report.reporterNickname}님이 신고</span>
                  <span className={styles.date}>{new Date(report.createdAt).toLocaleString()}</span>
                </div>

                <p className={styles.reason}>{report.reason}</p>

                {report.targetType === 'BOARD' && (
                  <Link className={styles.preview} to={`/boards/${report.targetId}`}>
                    {report.targetPreview}
                  </Link>
                )}
                {report.targetType === 'COMMENT' && report.targetBoardId && (
                  <Link className={styles.preview} to={`/boards/${report.targetBoardId}`}>
                    {report.targetPreview}
                  </Link>
                )}
                {(report.targetType === 'MEMBER' ||
                  report.targetType === 'REVIEW' ||
                  (report.targetType === 'COMMENT' && !report.targetBoardId)) && (
                  <p className={styles.preview}>{report.targetPreview}</p>
                )}

                <div className={styles.actions}>
                  {status === 'PENDING' && (
                    <button type="button" className={styles.resolveBtn} onClick={() => handleResolve(report.reportId)}>
                      <CheckIcon /> 처리 완료
                    </button>
                  )}
                  <button type="button" className={styles.deleteBtn} onClick={() => handleDelete(report.reportId)}>
                    <TrashIcon /> 삭제
                  </button>
                </div>
              </li>
            ))}
            {reportPage.content.length === 0 && (
              <div className={styles.empty}>
                <span className={styles.emptyIcon}>
                  <WarningIcon />
                </span>
                <p>해당 상태의 신고가 없습니다.</p>
              </div>
            )}
          </ul>
          <Pagination page={reportPage.number} totalPages={reportPage.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
