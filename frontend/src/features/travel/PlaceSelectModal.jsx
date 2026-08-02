import { useState } from 'react';
import { addTravelDetail } from './api';
import { useToast } from '../../hooks/useToast';
import Modal from '../../components/Modal/Modal';
import PlaceDetailModal from './PlaceDetailModal';
import { MapPinIcon } from '../../components/Icon/Icon';
import styles from './PlaceSelectModal.module.scss';

// 방문지 검색 - Place 지도(박영준 트랙)가 아직 없어서 임시 목업 데이터로 우선 구현.
// 나중에 실제 장소 검색으로 교체 예정
const MOCK_PLACES = [
  { placeId: 1, placeName: '한강공원', address: '서울 영등포구 여의동로 330' },
  { placeId: 2, placeName: '반려동물 동반 카페 몽실', address: '서울 마포구 어울마당로 1' },
  { placeId: 3, placeName: '펫프렌들리 해수욕장', address: '강원 강릉시 해안로 20' },
];

export default function PlaceSelectModal({ open, onClose, travelId, travelDate, onAdded }) {
  const { showToast } = useToast();
  const [selectedPlace, setSelectedPlace] = useState(null);

  async function handleAdd(place) {
    try {
      await addTravelDetail(travelId, { placeId: place.placeId, travelDate });
      setSelectedPlace(null);
      onAdded();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '방문지 추가에 실패했습니다.', 'error');
    }
  }

  return (
    <Modal open={open} onClose={onClose}>
      <div className={styles.modal}>
        <h2>방문지 추가</h2>
        <ul className={styles.list}>
          {MOCK_PLACES.map((place) => (
            <li key={place.placeId}>
              <button type="button" className={styles.item} onClick={() => setSelectedPlace(place)}>
                <span className={styles.itemIcon}>
                  <MapPinIcon />
                </span>
                <div>
                  <p className={styles.itemName}>{place.placeName}</p>
                  <p className={styles.itemAddress}>{place.address}</p>
                </div>
              </button>
            </li>
          ))}
        </ul>
      </div>

      <PlaceDetailModal
        open={Boolean(selectedPlace)}
        onClose={() => setSelectedPlace(null)}
        place={selectedPlace}
        onAdd={handleAdd}
      />
    </Modal>
  );
}
