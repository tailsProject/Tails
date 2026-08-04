import { useState } from 'react';
import { searchPlaces } from '../place/api';
import { addTravelDetail } from './api';
import { useToast } from '../../hooks/useToast';
import Modal from '../../components/Modal/Modal';
import PlaceDetailModal from './PlaceDetailModal';
import Button from '../../components/Button/Button';
import { MapPinIcon, MagnifyingGlassIcon } from '../../components/Icon/Icon';
import styles from './PlaceSelectModal.module.scss';

// 방문지 검색 - 키워드 검색 리스트로 /api/places/search에 연동
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
      const res = await searchPlaces({ keyword: keyword.trim() });
      setResults(res.data.data.content);
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
