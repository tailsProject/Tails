// 여행 일정 상세 페이지, 일자별 탭
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getTravelDetail, deleteTravel, shareTravel, unshareTravel } from './api';
import { useToast } from '../../hooks/useToast';
import { useConfirm } from '../../hooks/useConfirm';
import TravelFormModal from './TravelFormModal';
import DaySchedule from './DaySchedule';
import StateMessage from '../../components/StateMessage/StateMessage';
import { SuitcaseIcon, PencilIcon, TrashIcon, LinkIcon, XMarkIcon, CheckIcon } from '../../components/Icon/Icon';
import styles from './TravelDetailPage.module.scss';

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];

function formatLocalDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function getDateRange(start, end) {
  const dates = [];
  const current = new Date(`${start}T00:00:00`);
  const last = new Date(`${end}T00:00:00`);
  while (current <= last) {
    dates.push(formatLocalDate(current));
    current.setDate(current.getDate() + 1);
  }
  return dates;
}

export default function TravelDetailPage() {
  const { travelId } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const confirm = useConfirm();

  const [travel, setTravel] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [shareToken, setShareToken] = useState(null);
  const [shareBoxOpen, setShareBoxOpen] = useState(false);
  const [copied, setCopied] = useState(false);

  async function load() {
    try {
      const res = await getTravelDetail(travelId);
      const data = res.data.data;
      setTravel(data);
      setSelectedDate((prev) => (prev && prev >= data.startDate && prev <= data.endDate ? prev : data.startDate));
    } catch {
      setNotFound(true);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [travelId]);

  async function handleDelete() {
    const ok = await confirm('여행 일정을 삭제하시겠습니까?\n세부 일정도 함께 삭제되며 복구할 수 없습니다.');
    if (!ok) return;
    try {
      await deleteTravel(travelId);
      showToast('삭제되었습니다.', 'success');
      navigate('/travels');
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '삭제에 실패했습니다.', 'error');
    }
  }

  function handleEditSaved() {
    setEditModalOpen(false);
    load();
  }

  async function handleShare() {
    if (shareToken) {
      setShareBoxOpen((prev) => !prev);
      return;
    }
    try {
      const res = await shareTravel(travelId);
      setShareToken(res.data.data.shareToken);
      setShareBoxOpen(true);
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '공유 링크 생성에 실패했습니다.', 'error');
    }
  }

  async function handleUnshare() {
    const ok = await confirm('공유를 중단하시겠습니까?\n기존에 전달한 공유 링크는 더 이상 사용할 수 없습니다.');
    if (!ok) return;
    try {
      await unshareTravel(travelId);
      setShareToken(null);
      setShareBoxOpen(false);
      showToast('공유가 중단되었습니다.', 'success');
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '공유 중단에 실패했습니다.', 'error');
    }
  }

  async function handleCopyLink() {
    await navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    showToast('링크를 복사했습니다.', 'success');
    setTimeout(() => setCopied(false), 1500);
  }

  if (notFound) {
    return (
      <StateMessage
        icon={SuitcaseIcon}
        title="여행 일정을 찾을 수 없어요"
        description="삭제되었거나 존재하지 않는 여행이에요."
        actionTo="/travels"
        actionLabel="목록으로"
      />
    );
  }

  if (!travel) {
    return null;
  }

  const dates = getDateRange(travel.startDate, travel.endDate);
  const shareUrl = shareToken ? `${window.location.origin}/travels/shared/${shareToken}` : null;

  return (
    <div className={styles.wrapper}>
      <div className={styles.header}>
        <div>
          <h1>{travel.title}</h1>
          <p className={styles.dates}>
            {travel.startDate} ~ {travel.endDate}
          </p>
        </div>
        <div className={styles.headerActions}>
          <button
            type="button"
            className={styles.headerActionBtn}
            onClick={handleShare}
            aria-label={shareToken ? '공유 링크 보기' : '공유 링크 만들기'}
          >
            <LinkIcon />
          </button>
          <button type="button" className={styles.headerActionBtn} onClick={() => setEditModalOpen(true)} aria-label="수정">
            <PencilIcon />
          </button>
          <button type="button" className={styles.headerActionBtn} onClick={handleDelete} aria-label="삭제">
            <TrashIcon />
          </button>
        </div>
      </div>

      {shareBoxOpen && shareUrl && (
        <div className={styles.shareBox}>
          <span className={styles.shareBoxIcon}>
            <LinkIcon />
          </span>
          <input type="text" value={shareUrl} readOnly onFocus={(e) => e.target.select()} />
          <button
            type="button"
            onClick={handleCopyLink}
            className={copied ? `${styles.copyBtn} ${styles.copyBtnDone}` : styles.copyBtn}
          >
            {copied ? <CheckIcon /> : null}
            {copied ? '복사됨' : '복사'}
          </button>
          <span className={styles.shareBoxDivider} />
          <button type="button" onClick={handleUnshare} className={styles.unshareBtn} aria-label="공유 중단">
            <XMarkIcon />
          </button>
        </div>
      )}

      <div className={styles.dayTabs}>
        {dates.map((date, index) => {
          const weekday = WEEKDAYS[new Date(`${date}T00:00:00`).getDay()];
          const [, month, day] = date.split('-');
          return (
            <button
              key={date}
              className={date === selectedDate ? styles.dayActive : styles.day}
              onClick={() => setSelectedDate(date)}
            >
              <span className={styles.dayNum}>DAY {index + 1}</span>
              <span className={styles.dayDate}>
                {Number(month)}.{Number(day)} <em>{weekday}</em>
              </span>
            </button>
          );
        })}
      </div>

      {selectedDate && <DaySchedule travelId={travelId} date={selectedDate} />}

      <TravelFormModal
        open={editModalOpen}
        onClose={() => setEditModalOpen(false)}
        travelId={travelId}
        onSaved={handleEditSaved}
      />
    </div>
  );
}
