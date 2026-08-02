// 카카오맵 기반 장소 지도 페이지, 목록 연동 담당
import { useEffect, useRef, useState } from 'react';
import { loadKakaoMaps } from './kakaoLoader';
import { getPlaces, getPlaceRatingSummaries } from './api';
import Button from '../../components/Button/Button';
import { resolveImage } from '../../utils/resolveImage';
import { getCategoryLabel, getCategoryIconUrl } from '../../utils/placeCategory';
import StateMessage from '../../components/StateMessage/StateMessage';
import { MapIcon, PlusIcon, CheckIcon } from '../../components/Icon/Icon';
import styles from './PlaceMapPage.module.scss';

const KOREA_VIEW = { center: { lat: 35.8, lng: 127.8 }, level: 13 };

const MAX_ZOOM_OUT_LEVEL = 13;

const KOREA_BOUNDS = { minLat: 32, maxLat: 40, minLng: 124, maxLng: 132 };

export const PIN_MARKER_SVG =
  'data:image/svg+xml;charset=UTF-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="30" height="40" viewBox="0 0 30 40">' +
      '<path d="M15 0C6.716 0 0 6.716 0 15c0 11.25 15 25 15 25s15-13.75 15-25C30 6.716 23.284 0 15 0z" fill="#ff8a3d"/>' +
      '<circle cx="15" cy="15" r="6" fill="#fff"/></svg>',
  );

const ACTIVE_PIN_MARKER_SVG =
  'data:image/svg+xml;charset=UTF-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="30" height="40" viewBox="0 0 30 40">' +
      '<path d="M15 0C6.716 0 0 6.716 0 15c0 11.25 15 25 15 25s15-13.75 15-25C30 6.716 23.284 0 15 0z" fill="#e8590c"/>' +
      '<circle cx="15" cy="15" r="6" fill="#fff"/></svg>',
  );

export default function PlaceMapPage({ selectMode = false, onAddPlace, addedPlaceIds } = {}) {
  const mapContainerRef = useRef(null);
  const mapRef = useRef(null);
  const markersRef = useRef([]);
  const [places, setPlaces] = useState([]);
  const [ratingByPlaceId, setRatingByPlaceId] = useState({});
  const [sdkError, setSdkError] = useState(false);
  const [activeId, setActiveId] = useState(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [addingIds, setAddingIds] = useState(() => new Set());
  const markerByIdRef = useRef(new Map());
  const activeMarkerRef = useRef(null);
  const infoOverlayRef = useRef(null);
  const ratingByPlaceIdRef = useRef({});
  useEffect(() => {
    ratingByPlaceIdRef.current = ratingByPlaceId;
  }, [ratingByPlaceId]);
  const listItemRefs = useRef(new Map());
  const listPaneRef = useRef(null);
  const activeIdRef = useRef(null);
  useEffect(() => {
    activeIdRef.current = activeId;
  }, [activeId]);
  const loadMoreRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    loadKakaoMaps()
      .then((kakao) => {
        if (cancelled) return;
        const map = new kakao.maps.Map(mapContainerRef.current, {
          center: new kakao.maps.LatLng(KOREA_VIEW.center.lat, KOREA_VIEW.center.lng),
          level: KOREA_VIEW.level,
        });
        map.setMaxLevel(MAX_ZOOM_OUT_LEVEL);
        const dragMinLat = 33.3;
        kakao.maps.event.addListener(map, 'center_changed', () => {
          const center = map.getCenter();
          const lat = Math.min(Math.max(center.getLat(), dragMinLat), KOREA_BOUNDS.maxLat);
          const lng = Math.min(Math.max(center.getLng(), KOREA_BOUNDS.minLng), KOREA_BOUNDS.maxLng);
          if (lat !== center.getLat() || lng !== center.getLng()) {
            map.setCenter(new kakao.maps.LatLng(lat, lng));
          }
        });
        mapRef.current = { kakao, map };
        loadDefaultPlaces();
      })
      .catch(() => setSdkError(true));
    return () => {
      cancelled = true;
    };
  }, []);

  async function loadDefaultPlaces() {
    const res = await getPlaces({ page: 0, size: 50 });
    setPage(0);
    setHasMore(!res.data.data.last);
    applyPlaces(res.data.data.content, { fitToKorea: true });
    loadMoreRef.current = async (nextPage) => {
      const moreRes = await getPlaces({ page: nextPage, size: 50 });
      setPage(nextPage);
      setHasMore(!moreRes.data.data.last);
      applyPlaces(moreRes.data.data.content, { append: true, fitToKorea: true });
    };
  }

  async function handleLoadMore() {
    if (!loadMoreRef.current) return;
    setIsLoadingMore(true);
    try {
      await loadMoreRef.current(page + 1);
    } finally {
      setIsLoadingMore(false);
    }
  }

  function applyPlaces(placeList, { append = false, fitToKorea = false } = {}) {
    setPlaces((prev) => (append ? [...prev, ...placeList] : placeList));
    loadRatingSummaries(placeList);
    if (!mapRef.current) return;
    const { kakao, map } = mapRef.current;

    if (!append) {
      markersRef.current.forEach((marker) => marker.setMap(null));
      markersRef.current = [];
      markerByIdRef.current = new Map();
      activeMarkerRef.current = null;
      hideInfoOverlay();
    }

    const withCoords = placeList.filter(
      (place) =>
        place.latitude != null &&
        place.longitude != null &&
        place.latitude >= KOREA_BOUNDS.minLat &&
        place.latitude <= KOREA_BOUNDS.maxLat &&
        place.longitude >= KOREA_BOUNDS.minLng &&
        place.longitude <= KOREA_BOUNDS.maxLng
    );
    const image = new kakao.maps.MarkerImage(PIN_MARKER_SVG, new kakao.maps.Size(24, 32), { offset: new kakao.maps.Point(12, 32) });
    const newMarkers = withCoords.map((place) => {
      const position = new kakao.maps.LatLng(place.latitude, place.longitude);
      const marker = new kakao.maps.Marker({ position, image, map });
      markerByIdRef.current.set(place.placeId, { marker, image });
      kakao.maps.event.addListener(marker, 'click', () => {
        if (activeIdRef.current === place.placeId) {
          clearSelection();
          return;
        }
        setActiveId(place.placeId);
        highlightMarker(place.placeId);
        showInfoOverlay(place);
        scrollListToPlace(place.placeId);
      });
      return marker;
    });
    markersRef.current = [...markersRef.current, ...newMarkers];

    if (fitToKorea) {
      map.setCenter(new kakao.maps.LatLng(KOREA_VIEW.center.lat, KOREA_VIEW.center.lng));
      map.setLevel(KOREA_VIEW.level);
    } else if (withCoords.length > 0) {
      const bounds = new kakao.maps.LatLngBounds();
      const boundsSource = append ? markersRef.current.map((marker) => marker.getPosition()) : withCoords.map((place) => new kakao.maps.LatLng(place.latitude, place.longitude));
      boundsSource.forEach((position) => bounds.extend(position));
      map.setBounds(bounds);
    }
  }

  async function loadRatingSummaries(placeList) {
    const placeIds = placeList.map((place) => place.placeId);
    if (placeIds.length === 0) return;
    try {
      const res = await getPlaceRatingSummaries(placeIds);
      setRatingByPlaceId((prev) => {
        const next = { ...prev };
        for (const summary of res.data.data) {
          next[summary.placeId] = summary;
        }
        return next;
      });
    } catch {
    }
  }

  async function handleAdd(place) {
    if (addingIds.has(place.placeId)) return;
    setAddingIds((prev) => new Set(prev).add(place.placeId));
    try {
      await onAddPlace(place);
    } finally {
      setAddingIds((prev) => {
        const next = new Set(prev);
        next.delete(place.placeId);
        return next;
      });
    }
  }

  function scrollListToPlace(placeId) {
    const pane = listPaneRef.current;
    const item = listItemRefs.current.get(placeId);
    if (!pane || !item) return;
    const delta = item.getBoundingClientRect().top - pane.getBoundingClientRect().top;
    pane.scrollTo({
      top: pane.scrollTop + delta - pane.clientHeight / 2 + item.clientHeight / 2,
      behavior: 'smooth',
    });
  }

  function highlightMarker(placeId) {
    if (!mapRef.current) return;
    const { kakao } = mapRef.current;
    const prev = activeMarkerRef.current;
    if (prev) {
      prev.marker.setImage(prev.image);
      prev.marker.setZIndex(0);
    }
    const entry = markerByIdRef.current.get(placeId);
    activeMarkerRef.current = entry ?? null;
    if (!entry) return;
    entry.marker.setImage(
      new kakao.maps.MarkerImage(ACTIVE_PIN_MARKER_SVG, new kakao.maps.Size(30, 40), {
        offset: new kakao.maps.Point(15, 40),
      }),
    );
    entry.marker.setZIndex(10);
  }

  function buildInfoOverlayContent(place) {
    const wrapper = document.createElement('div');
    wrapper.className = styles.infoOverlay;

    const closeButton = document.createElement('button');
    closeButton.type = 'button';
    closeButton.className = styles.infoOverlayClose;
    closeButton.textContent = '✕';
    closeButton.setAttribute('aria-label', '닫기');
    closeButton.onclick = (e) => {
      e.stopPropagation();
      clearSelection();
    };
    wrapper.appendChild(closeButton);

    const title = document.createElement('p');
    title.className = styles.infoOverlayTitle;
    title.textContent = place.placeName;
    wrapper.appendChild(title);

    const categoryLabel = getCategoryLabel(place);
    const rating = ratingByPlaceIdRef.current[place.placeId];
    if (categoryLabel || rating) {
      const metaRow = document.createElement('div');
      metaRow.className = styles.infoOverlayMeta;
      if (categoryLabel) {
        const badge = document.createElement('span');
        badge.className = styles.infoOverlayBadge;
        badge.textContent = categoryLabel;
        metaRow.appendChild(badge);
      }
      if (rating) {
        const ratingEl = document.createElement('span');
        ratingEl.className = styles.infoOverlayRating;
        ratingEl.textContent = `★ ${rating.averageRating.toFixed(1)} (${rating.reviewCount})`;
        metaRow.appendChild(ratingEl);
      }
      wrapper.appendChild(metaRow);
    }

    return wrapper;
  }

  function showInfoOverlay(place) {
    if (!mapRef.current || place.latitude == null || place.longitude == null) return;
    const { kakao, map } = mapRef.current;
    hideInfoOverlay();
    const overlay = new kakao.maps.CustomOverlay({
      position: new kakao.maps.LatLng(place.latitude, place.longitude),
      content: buildInfoOverlayContent(place),
      yAnchor: 1.35,
      zIndex: 20,
    });
    overlay.setMap(map);
    infoOverlayRef.current = overlay;
  }

  function hideInfoOverlay() {
    infoOverlayRef.current?.setMap(null);
    infoOverlayRef.current = null;
  }

  function clearSelection() {
    setActiveId(null);
    highlightMarker(null);
    hideInfoOverlay();
  }

  function focusPlace(place) {
    setActiveId(place.placeId);
    if (!mapRef.current || place.latitude == null || place.longitude == null) return;
    const { kakao, map } = mapRef.current;
    highlightMarker(place.placeId);
    showInfoOverlay(place);
    const position = new kakao.maps.LatLng(place.latitude, place.longitude);
    map.setLevel(5, { anchor: position });
    map.setCenter(position);
  }

  return (
    <div className={selectMode ? `${styles.page} ${styles.pageEmbedded}` : styles.page}>
      <div className={styles.wrapper}>
        <aside className={styles.listPane} ref={listPaneRef}>
          <p className={styles.resultCount}>{places.length}곳의 장소</p>
          <ul className={styles.list}>
            {places.map((place) => (
              <li
                key={place.placeId}
                ref={(el) => {
                  if (el) listItemRefs.current.set(place.placeId, el);
                  else listItemRefs.current.delete(place.placeId);
                }}
              >
                <button
                  onClick={() => (activeId === place.placeId ? clearSelection() : focusPlace(place))}
                  className={`${styles.item} ${activeId === place.placeId ? styles.itemActive : ''}`}
                >
                  <div className={styles.thumb}>
                    {resolveImage(place.imageUrl) ? (
                      <img src={resolveImage(place.imageUrl)} alt="" />
                    ) : (
                      <img src={getCategoryIconUrl(place)} alt="" />
                    )}
                  </div>
                  <div className={styles.itemBody}>
                    <span className={styles.titleRow}>
                      <span className={styles.title}>{place.placeName}</span>
                      {getCategoryLabel(place) && (
                        <span className={styles.categoryBadge}>{getCategoryLabel(place)}</span>
                      )}
                    </span>
                    {ratingByPlaceId[place.placeId] && (
                      <span className={styles.rating}>
                        ★ {ratingByPlaceId[place.placeId].averageRating.toFixed(1)}
                        <span className={styles.ratingCount}>({ratingByPlaceId[place.placeId].reviewCount})</span>
                      </span>
                    )}
                    <span className={styles.meta}>
                      {place.address}
                      {place.distanceMeters != null && ` · ${Math.round(place.distanceMeters)}m`}
                    </span>
                  </div>
                  {selectMode && (
                    <span
                      className={addedPlaceIds?.has(place.placeId) ? styles.addedBadge : styles.addBadge}
                      onClick={(e) => {
                        e.stopPropagation();
                        if (!addedPlaceIds?.has(place.placeId)) handleAdd(place);
                      }}
                    >
                      {addedPlaceIds?.has(place.placeId) ? <CheckIcon /> : <PlusIcon />}
                    </span>
                  )}
                </button>
              </li>
            ))}
            {places.length === 0 && <p className={styles.empty}>검색 결과가 없습니다.</p>}
          </ul>
          {hasMore && (
            <Button variant="secondary" onClick={handleLoadMore} disabled={isLoadingMore} className={styles.loadMore}>
              {isLoadingMore ? '불러오는 중...' : '더보기'}
            </Button>
          )}
        </aside>

        <div className={styles.mapPane}>
          {sdkError ? (
            <div className={styles.sdkError}>
              <StateMessage
                icon={MapIcon}
                title="지도를 불러오지 못했어요"
                description="카카오맵 키/도메인 설정을 확인해주세요."
              />
            </div>
          ) : (
            <div ref={mapContainerRef} className={styles.map} />
          )}
        </div>
      </div>
    </div>
  );
}
