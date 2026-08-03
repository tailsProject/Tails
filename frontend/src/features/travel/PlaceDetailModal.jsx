import Modal from '../../components/Modal/Modal';
import Button from '../../components/Button/Button';
import styles from './PlaceDetailModal.module.scss';

// 방문지 상세 - 장소 지도(박영준 트랙)가 아직 없어서 임시 목업으로 표시
export default function PlaceDetailModal({ open, onClose, place, onAdd }) {
  if (!place) {
    return null;
  }

  return (
    <Modal open={open} onClose={onClose}>
      <div className={styles.modal}>
        <h2>{place.placeName}</h2>
        <p className={styles.address}>{place.address}</p>
        <div className={styles.actions}>
          <Button type="button" variant="secondary" onClick={onClose}>
            닫기
          </Button>
          <Button type="button" onClick={() => onAdd(place)}>
            방문지로 추가
          </Button>
        </div>
      </div>
    </Modal>
  );
}
