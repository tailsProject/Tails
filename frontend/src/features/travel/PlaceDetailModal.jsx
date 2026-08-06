// 일정 화면에서 방문지 상세를 모달로 보여줌
import Modal from '../../components/Modal/Modal';
import PlaceDetailContent from '../place/PlaceDetailContent';
import styles from './PlaceDetailModal.module.scss';

export default function PlaceDetailModal({ open, placeId, onClose }) {
  return (
    <Modal open={open} onClose={onClose}>
      <div className={styles.modal}>{placeId && <PlaceDetailContent placeId={placeId} onClose={onClose} />}</div>
    </Modal>
  );
}
