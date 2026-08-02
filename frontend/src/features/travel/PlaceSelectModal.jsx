import { useState } from 'react';
import { searchPlaces, addTravelDetail } from './api';
import { useToast } from '../../hooks/useToast';
import Modal from '../../components/Modal/Modal';
import PlaceDetailModal from './PlaceDetailModal';
import Button from '../../components/Button/Button';
import { MapPinIcon, MagnifyingGlassIcon } from '../../components/Icon/Icon';
import styles from './PlaceSelectModal.module.scss';

// 방문지 검색 - Place 지도(박영준 트랙)가 아직 없어서 키워드 검색 리스트 뼈대로 실제
// 백엔드(/api/places/search)에 연동. 나중에 실제 지도 UI로 교체 예정
export default function PlaceSelectModal({ open, onClose, travelId, travelDate, onAdded }) {
  const { showToast } = useToast();
  const [keyword, setKeyword] = useState('');
  const [results, setResults] = useState(null);
  const [searching, setSearching] = useState(false);
  const [selectedPlace, setSelectedPlace] = useState(null);

  async function handleSearch(e) {
    e.preventDefault();
    if (!keyword.trim()) return;
    setSearching(true);
    try {
      const res = await searchPlaces(keyword.trim());
      setResults(res.data.data);
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '검색에 실패했습니다.', 'error');
    } finally {
      setSearching(false);
    }
  }

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
        <form onSubmit={handleSearch} className={styles.searchRow}>
          <input
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="장소명, 지역으로 검색"
          />
          <Button type="submit" variant="secondary" disabled={searching}>
            <MagnifyingGlassIcon />
          </Button>
        </form>

        <ul className={styles.list}>
          {(results ?? []).map((place) => (
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
          {results && results.length === 0 && <p className={styles.empty}>검색 결과가 없습니다.</p>}
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
