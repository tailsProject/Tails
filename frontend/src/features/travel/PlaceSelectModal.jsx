// 지도 페이지를 재사용해 방문지를 검색하고 추가하는 모달
import Modal from '../../components/Modal/Modal';
import Button from '../../components/Button/Button';
import PlaceMapPage from '../place/PlaceMapPage';
import styles from './PlaceSelectModal.module.scss';

export default function PlaceSelectModal({ open, onClose, addedPlaceIds, onAddPlace }) {
  return (
    <Modal open={open} onClose={onClose}>
      <div className={styles.modal}>
        <div className={styles.header}>
          <h2>방문지 추가</h2>
          <Button type="button" onClick={onClose}>
            완료
          </Button>
        </div>
        <div className={styles.body}>
          <PlaceMapPage selectMode onAddPlace={onAddPlace} addedPlaceIds={addedPlaceIds} />
        </div>
      </div>
    </Modal>
  );
}
