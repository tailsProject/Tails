// 여행 일정 상세 페이지, 일자별 탭
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getTravelDetail, getTravelDetails, deleteTravel, deleteTravelDetail } from './api';
import { useToast } from '../../hooks/useToast';
import { useConfirm } from '../../hooks/useConfirm';
import TravelFormModal from './TravelFormModal';
import PlaceSelectModal from './PlaceSelectModal';
import StateMessage from '../../components/StateMessage/StateMessage';
import { SuitcaseIcon, PencilIcon, TrashIcon, PlusIcon, MapPinIcon } from '../../components/Icon/Icon';
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
  const [details, setDetails] = useState([]);
  const [notFound, setNotFound] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [placeModalOpen, setPlaceModalOpen] = useState(false);

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

  async function loadDetails(date) {
    if (!date) return;
    const res = await getTravelDetails(travelId, date);
    setDetails(res.data.data);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [travelId]);

  useEffect(() => {
    loadDetails(selectedDate);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [travelId, selectedDate]);

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

  async function handleDeleteDetail(detailId) {
    const ok = await confirm('방문지를 삭제하시겠습니까?');
    if (!ok) return;
    try {
      await deleteTravelDetail(travelId, detailId);
      loadDetails(selectedDate);
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '삭제에 실패했습니다.', 'error');
    }
  }

  function handleEditSaved() {
    setEditModalOpen(false);
    load();
  }

  function handlePlaceAdded() {
    setPlaceModalOpen(false);
    loadDetails(selectedDate);
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
          <button type="button" className={styles.headerActionBtn} onClick={() => setEditModalOpen(true)} aria-label="수정">
            <PencilIcon />
          </button>
          <button type="button" className={styles.headerActionBtn} onClick={handleDelete} aria-label="삭제">
            <TrashIcon />
          </button>
        </div>
      </div>

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

      <div className={styles.detailList}>
        {details.map((detail) => (
          <div key={detail.detailId} className={styles.detailItem}>
            <span className={styles.detailIcon}>
              <MapPinIcon />
            </span>
            <div className={styles.detailBody}>
              <p className={styles.placeName}>{detail.placeName}</p>
              {detail.memo && <p className={styles.memo}>{detail.memo}</p>}
            </div>
            <button
              type="button"
              className={styles.detailDeleteBtn}
              onClick={() => handleDeleteDetail(detail.detailId)}
              aria-label="방문지 삭제"
            >
              <TrashIcon />
            </button>
          </div>
        ))}
        <button type="button" className={styles.addPlaceBtn} onClick={() => setPlaceModalOpen(true)}>
          <PlusIcon /> 방문지 추가
        </button>
      </div>

      <TravelFormModal
        open={editModalOpen}
        onClose={() => setEditModalOpen(false)}
        travelId={travelId}
        onSaved={handleEditSaved}
      />

      <PlaceSelectModal
        open={placeModalOpen}
        onClose={() => setPlaceModalOpen(false)}
        travelId={travelId}
        travelDate={selectedDate}
        onAdded={handlePlaceAdded}
      />
    </div>
  );
}
