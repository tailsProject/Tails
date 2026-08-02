// 장소 상세 본문, 지도 페이지 오버레이와 독립 페이지에서 함께 사용
import { useEffect, useRef, useState } from 'react';
import { getPlaceDetail, togglePlaceBookmark } from './api';
import { loadKakaoMaps } from './kakaoLoader';
import { PIN_MARKER_SVG } from './PlaceMapPage';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { getCategoryLabel } from '../../utils/placeCategory';
import { resolveImage } from '../../utils/resolveImage';
import ReviewSection from './ReviewSection';
import StateMessage from '../../components/StateMessage/StateMessage';
import { HeartIcon, MapPinIcon } from '../../components/Icon/Icon';
import styles from './PlaceDetailPage.module.scss';

export default function PlaceDetailContent({ placeId, onClose }) {
  const { isAuthenticated } = useAuth();
  const { showToast } = useToast();
  const [place, setPlace] = useState(null);
  const [bookmarked, setBookmarked] = useState(false);
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const [notFound, setNotFound] = useState(false);
  const mapContainerRef = useRef(null);

  useEffect(() => {
    setPlace(null);
    setNotFound(false);
    setActiveImageIndex(0);
    getPlaceDetail(placeId)
      .then((res) => {
        setPlace(res.data.data);
        setBookmarked(res.data.data.bookmarked);
      })
      .catch(() => setNotFound(true));
  }, [placeId]);

  useEffect(() => {
    if (!place || place.latitude == null || place.longitude == null) return;
    loadKakaoMaps().then((kakao) => {
      const map = new kakao.maps.Map(mapContainerRef.current, {
        center: new kakao.maps.LatLng(place.latitude, place.longitude),
        level: 4,
      });
      new kakao.maps.Marker({
        position: new kakao.maps.LatLng(place.latitude, place.longitude),
        map,
        image: new kakao.maps.MarkerImage(PIN_MARKER_SVG, new kakao.maps.Size(24, 32), {
          offset: new kakao.maps.Point(12, 32),
        }),
      });
    });
  }, [place]);

  async function handleBookmark() {
    if (!isAuthenticated) {
      showToast('로그인이 필요합니다.', 'error');
      return;
    }
    try {
      const res = await togglePlaceBookmark(placeId);
      setBookmarked(res.data.data.bookmarked);
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '찜 처리에 실패했습니다.', 'error');
    }
  }

  if (notFound) {
    return (
      <StateMessage
        icon={MapPinIcon}
        title="장소를 찾을 수 없어요"
        description="삭제되었거나 존재하지 않는 장소예요."
        {...(onClose ? { onAction: onClose, actionLabel: '닫기' } : { actionTo: '/places', actionLabel: '지도로 돌아가기' })}
      />
    );
  }

  if (!place) {
    return null;
  }

  const images = [place.imageUrl, ...(place.imageUrls ?? [])].filter(
    (url, index, all) => url && all.indexOf(url) === index
  );
  const categoryLabel = getCategoryLabel(place);

  return (
    <div className={styles.wrapper}>
      <div className={styles.header}>
        <div className={styles.titleGroup}>
          <h1>{place.placeName}</h1>
          {categoryLabel && <span className={styles.categoryBadge}>{categoryLabel}</span>}
        </div>
        <div className={styles.headerActions}>
          <button
            onClick={handleBookmark}
            className={`${styles.bookmarkButton} ${bookmarked ? styles.active : ''}`}
            aria-label={bookmarked ? '찜 해제' : '찜하기'}
          >
            <HeartIcon fill={bookmarked ? 'currentColor' : 'none'} />
          </button>
          {onClose && (
            <button type="button" onClick={onClose} className={styles.closeButton} aria-label="닫기">
              ✕
            </button>
          )}
        </div>
      </div>

      {images.length > 0 && (
        <div className={styles.gallery}>
          <img src={resolveImage(images[activeImageIndex])} alt="" className={styles.thumbnail} />
          {images.length > 1 && (
            <div className={styles.thumbList}>
              {images.map((url, index) => (
                <button
                  key={url}
                  type="button"
                  className={index === activeImageIndex ? styles.thumbActive : styles.thumb}
                  onClick={() => setActiveImageIndex(index)}
                >
                  <img src={resolveImage(url)} alt="" />
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      <dl className={styles.infoList}>
        <dt>주소</dt>
        <dd>{place.address}</dd>
        {place.phone && (
          <>
            <dt>전화번호</dt>
            <dd>{place.phone}</dd>
          </>
        )}
        {place.petInfo && (
          <>
            <dt>반려동물 동반 정보</dt>
            <dd>{place.petInfo}</dd>
          </>
        )}
      </dl>

      {place.latitude != null && place.longitude != null && <div ref={mapContainerRef} className={styles.map} />}

      <ReviewSection placeId={placeId} />
    </div>
  );
}
