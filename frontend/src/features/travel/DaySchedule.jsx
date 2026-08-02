// 하루 단위 방문지 일정 - 드래그앤드롭으로 순서 재정렬
import { useEffect, useMemo, useRef, useState } from 'react';
import { getTravelDetails, deleteTravelDetail, reorderTravelDetails } from './api';
import { useToast } from '../../hooks/useToast';
import { useConfirm } from '../../hooks/useConfirm';
import Button from '../../components/Button/Button';
import PlaceSelectModal from './PlaceSelectModal';
import { MapPinIcon, PlusIcon, TrashIcon } from '../../components/Icon/Icon';
import styles from './DaySchedule.module.scss';

const STOP_COLORS = ['#ff8a3d', '#2f9e8f', '#7c6bdb', '#e5722a', '#1d8fc4', '#e04060'];

export default function DaySchedule({ travelId, date }) {
  const { showToast } = useToast();
  const confirm = useConfirm();
  const [details, setDetails] = useState([]);
  const [addModalOpen, setAddModalOpen] = useState(false);
  const dragIndexRef = useRef(null);

  const sortedDetails = useMemo(() => [...details].sort((a, b) => a.sequence - b.sequence), [details]);

  async function load() {
    const res = await getTravelDetails(travelId, date);
    setDetails(res.data.data);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [travelId, date]);

  async function handleDelete(detailId) {
    const ok = await confirm('이 방문지를 일정에서 삭제하시겠습니까?');
    if (!ok) return;
    await deleteTravelDetail(travelId, detailId);
    load();
  }

  function handleDragStart(index) {
    dragIndexRef.current = index;
  }

  function handleDragOver(e) {
    e.preventDefault();
  }

  async function handleDrop(targetIndex) {
    const fromIndex = dragIndexRef.current;
    dragIndexRef.current = null;
    if (fromIndex === null || fromIndex === targetIndex) return;

    const reordered = [...sortedDetails];
    const [moved] = reordered.splice(fromIndex, 1);
    reordered.splice(targetIndex, 0, moved);
    const renumbered = reordered.map((d, i) => ({ ...d, sequence: i + 1 }));
    setDetails(renumbered);

    try {
      await reorderTravelDetails(travelId, { travelDate: date, detailIds: renumbered.map((d) => d.detailId) });
    } catch {
      showToast('순서 변경에 실패했습니다.', 'error');
      load();
    }
  }

  function handlePlaceAdded() {
    setAddModalOpen(false);
    load();
  }

  return (
    <div className={styles.wrapper}>
      <ul className={styles.list}>
        {sortedDetails.map((detail, index) => (
          <li key={detail.detailId}>
            <div
              className={styles.item}
              draggable
              onDragStart={() => handleDragStart(index)}
              onDragOver={handleDragOver}
              onDrop={() => handleDrop(index)}
            >
              <span className={styles.sequence} style={{ backgroundColor: STOP_COLORS[index % STOP_COLORS.length] }}>
                {index + 1}
              </span>
              <span className={styles.itemIcon}>
                <MapPinIcon />
              </span>
              <div className={styles.itemBody}>
                <p className={styles.placeName}>{detail.placeName}</p>
                {detail.memo && <p className={styles.memo}>{detail.memo}</p>}
              </div>
              <button
                type="button"
                className={styles.itemDeleteBtn}
                onClick={() => handleDelete(detail.detailId)}
                aria-label="삭제"
              >
                <TrashIcon />
              </button>
            </div>
          </li>
        ))}
        {details.length === 0 && (
          <div className={styles.empty}>
            <p className={styles.emptyIcon}>
              <MapPinIcon />
            </p>
            <p>이 날짜에 등록된 방문지가 없습니다.</p>
          </div>
        )}
      </ul>

      <Button variant="secondary" onClick={() => setAddModalOpen(true)} className={styles.addPlaceBtn}>
        <PlusIcon /> 방문지 추가
      </Button>

      <PlaceSelectModal
        open={addModalOpen}
        onClose={() => setAddModalOpen(false)}
        travelId={travelId}
        travelDate={date}
        onAdded={handlePlaceAdded}
      />
    </div>
  );
}
