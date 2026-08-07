// 카카오맵 기반 장소 지도 페이지, 검색과 필터, 목록 연동 담당
import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { loadKakaoMaps } from './kakaoLoader';
import { getPlaces, searchPlaces, getPlaceRatingSummaries, autocompletePlaces, getPlacesRankedByRating } from './api';
import { getMyBookmarkedPlaces } from '../mypage/api';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import Button from '../../components/Button/Button';
import { resolveImage } from '../../utils/resolveImage';
import { CATEGORIES, getCategoryLabel, getCategoryIconUrl, toCat1Param } from '../../utils/placeCategory';
import { parseSearchKeyword } from '../../utils/parseSearchKeyword';
import PlaceDetailContent from './PlaceDetailContent';
import StateMessage from '../../components/StateMessage/StateMessage';
import { MagnifyingGlassIcon, MapIcon, MapPinIcon, PlusIcon, CheckIcon } from '../../components/Icon/Icon';
import styles from './PlaceMapPage.module.scss';

const KOREA_VIEW = { center: { lat: 35.8, lng: 127.8 }, level: 13 };

const MAX_ZOOM_OUT_LEVEL = 13;

const KOREA_BOUNDS = { minLat: 32, maxLat: 40, minLng: 124, maxLng: 132 };

const NEARBY_RADIUS_OPTIONS = [
  { value: 1000, label: '1km', level: 4 },
  { value: 3000, label: '3km', level: 5 },
  { value: 5000, label: '5km', level: 6 },
  { value: 10000, label: '10km', level: 7 },
];
const NEARBY_RADIUS_DEFAULT = 3000;

function distanceMeters(lat1, lng1, lat2, lng2) {
  const EARTH_RADIUS_M = 6371000;
  const toRad = (deg) => (deg * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

const REGIONS = [
  '서울', '부산', '대구', '인천', '광주', '대전', '울산', '세종',
  '경기', '강원', '충북', '충남', '전북', '전남', '경북', '경남', '제주',
];

const LOCAL_KEYWORD_SUGGESTIONS = [
  ...REGIONS.map((label) => ({ type: 'region', value: label, label })),
  ...CATEGORIES.filter((c) => c.key).map((c) => ({ type: 'category', value: c.key, label: c.label })),
];
const AUTOCOMPLETE_DEBOUNCE_MS = 250;

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

const MY_LOCATION_MARKER_SVG =
  'data:image/svg+xml;charset=UTF-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">' +
      '<circle cx="12" cy="12" r="11" fill="#4285f4" fill-opacity="0.25"/>' +
      '<circle cx="12" cy="12" r="6" fill="#4285f4" stroke="#ffffff" stroke-width="2.5"/></svg>',
  );

export default function PlaceMapPage({ selectMode = false, onAddPlace, addedPlaceIds } = {}) {
  const { showToast } = useToast();
  const { isAuthenticated } = useAuth();
  const [showBookmarks, setShowBookmarks] = useState(false);
  const [showRatingRanking, setShowRatingRanking] = useState(false);
  const mapContainerRef = useRef(null);
  const mapRef = useRef(null);
  const markersRef = useRef([]);
  const clustererRef = useRef(null);
  const [searchParams, setSearchParams] = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '');
  const [region, setRegion] = useState(searchParams.get('region') || '');
  const [category, setCategory] = useState(searchParams.get('category') || '');
  const [places, setPlaces] = useState([]);
  const [ratingByPlaceId, setRatingByPlaceId] = useState({});
  const [sdkError, setSdkError] = useState(false);
  const [activeId, setActiveId] = useState(null);
  const [selectedPlaceId, setSelectedPlaceId] = useState(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [isRegionOpen, setIsRegionOpen] = useState(false);
  const [isKeywordSuggestOpen, setIsKeywordSuggestOpen] = useState(false);
  // 서버에서 받아온 장소명 자동완성 후보
  const [placeSuggestions, setPlaceSuggestions] = useState([]);
  // 자동완성 드롭다운에서 방향키로 짚은 위치, 기본값 -1
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  // 방향키로 짚은 위치가 스크롤 밖으로 나가면 자동 스크롤용 DOM 참조
  const suggestItemRefs = useRef(new Map());
  useEffect(() => {
    if (highlightedIndex < 0) return;
    suggestItemRefs.current.get(highlightedIndex)?.scrollIntoView({ block: 'nearest' });
  }, [highlightedIndex]);
  const [addingIds, setAddingIds] = useState(() => new Set());
  const [radiusMode, setRadiusMode] = useState(null);
  const [nearbyRadius, setNearbyRadius] = useState(NEARBY_RADIUS_DEFAULT);
  const nearbyCoordsRef = useRef(null);
  const radiusModeRef = useRef(null);
  const categoryRef = useRef(category);
  useEffect(() => {
    radiusModeRef.current = radiusMode;
  }, [radiusMode]);
  useEffect(() => {
    categoryRef.current = category;
  }, [category]);
  const searchSeqRef = useRef(0);
  const markerByIdRef = useRef(new Map());
  const activeMarkerRef = useRef(null);
  const infoOverlayRef = useRef(null);
  const ratingByPlaceIdRef = useRef({});
  useEffect(() => {
    ratingByPlaceIdRef.current = ratingByPlaceId;
  }, [ratingByPlaceId]);
  const myLocationRef = useRef(null);
  const listItemRefs = useRef(new Map());
  const listPaneRef = useRef(null);
  const skipNextIdleRef = useRef(false);
  const activeIdRef = useRef(null);
  useEffect(() => {
    activeIdRef.current = activeId;
  }, [activeId]);
  useEffect(() => {
    if (radiusMode !== 'nearby' && myLocationRef.current) {
      myLocationRef.current.marker.setMap(null);
      myLocationRef.current.circle.setMap(null);
      myLocationRef.current = null;
    }
  }, [radiusMode]);
  const regionDropdownRef = useRef(null);
  const loadMoreRef = useRef(null);

  useEffect(() => {
    if (!isRegionOpen) return;
    function handleClickOutside(e) {
      if (regionDropdownRef.current && !regionDropdownRef.current.contains(e.target)) {
        setIsRegionOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isRegionOpen]);

  const keywordSuggestRef = useRef(null);
  useEffect(() => {
    if (!isKeywordSuggestOpen) return;
    function handleClickOutside(e) {
      if (keywordSuggestRef.current && !keywordSuggestRef.current.contains(e.target)) {
        setIsKeywordSuggestOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isKeywordSuggestOpen]);

  // 장소명 자동완성, 타이핑 멈춘 뒤 디바운스로 서버 조회
  // 시퀀스 번호로 응답 순서 어긋남 방지
  const autocompleteSeqRef = useRef(0);
  useEffect(() => {
    const trimmed = keyword.trim();
    if (!trimmed) {
      setPlaceSuggestions([]);
      return;
    }
    const seq = ++autocompleteSeqRef.current;
    const timeoutId = setTimeout(() => {
      autocompletePlaces(trimmed)
        .then((res) => {
          if (seq !== autocompleteSeqRef.current) return;
          setPlaceSuggestions(res.data.data);
        })
        .catch(() => {
          if (seq === autocompleteSeqRef.current) setPlaceSuggestions([]);
        });
    }, AUTOCOMPLETE_DEBOUNCE_MS);
    return () => clearTimeout(timeoutId);
  }, [keyword]);

  // 지역/카테고리 로컬 후보와 장소명 서버 후보 병합
  const keywordSuggestions = keyword.trim()
    ? [
        ...LOCAL_KEYWORD_SUGGESTIONS.filter((s) => s.label.includes(keyword.trim())).slice(0, 5),
        ...placeSuggestions.map((p) => ({ type: 'place', value: p.placeName, label: p.placeName, address: p.address })),
      ]
    : [];

  // 후보 목록이 바뀌면 방향키 선택 위치 초기화
  useEffect(() => {
    setHighlightedIndex(-1);
  }, [keyword]);

  // 방향키로 후보 탐색, 엔터로 선택 검색
  function handleKeywordKeyDown(e) {
    if (!isKeywordSuggestOpen || keywordSuggestions.length === 0) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setHighlightedIndex((prev) => (prev + 1) % keywordSuggestions.length);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlightedIndex((prev) => (prev <= 0 ? keywordSuggestions.length - 1 : prev - 1));
    } else if (e.key === 'Enter' && highlightedIndex >= 0) {
      // 후보 선택 상태에서만 엔터 가로채기, 아니면 일반 검색 제출로 넘김
      e.preventDefault();
      handleSuggestionSelect(keywordSuggestions[highlightedIndex]);
    }
  }

  function handleSuggestionSelect(suggestion) {
    setIsKeywordSuggestOpen(false);
    setPlaceSuggestions([]);
    setKeyword('');
    if (suggestion.type === 'region') {
      setRegion(suggestion.value);
      runSearch({ keyword: '', region: suggestion.value });
    } else if (suggestion.type === 'category') {
      setCategory(suggestion.value);
      runSearch({ keyword: '', categoryKey: suggestion.value });
    } else {
      // 장소명 후보 선택 시 기존 지역/카테고리 필터 초기화
      setRegion('');
      setCategory('');
      runSearch({ keyword: suggestion.value, region: '', categoryKey: '' });
    }
  }

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
        kakao.maps.event.addListener(map, 'idle', () => {
          if (skipNextIdleRef.current) {
            skipNextIdleRef.current = false;
            return;
          }
          if (radiusModeRef.current !== 'area') return;
          const { lat, lng, radius } = currentMapRadius();
          searchByRadius(lat, lng, radius, categoryRef.current);
        });
        clustererRef.current = new kakao.maps.MarkerClusterer({
          map,
          averageCenter: true,
          minLevel: 6,
          styles: [
            {
              width: '32px',
              height: '32px',
              background: 'rgba(255, 138, 61, 0.85)',
              borderRadius: '16px',
              color: '#fff',
              textAlign: 'center',
              lineHeight: '33px',
              fontWeight: '700',
            },
            {
              width: '42px',
              height: '42px',
              background: 'rgba(255, 138, 61, 0.9)',
              borderRadius: '21px',
              color: '#fff',
              textAlign: 'center',
              lineHeight: '43px',
              fontWeight: '700',
            },
            {
              width: '52px',
              height: '52px',
              background: 'rgba(229, 114, 42, 0.92)',
              borderRadius: '26px',
              color: '#fff',
              textAlign: 'center',
              lineHeight: '53px',
              fontWeight: '700',
            },
          ],
        });
        if (searchParams.get('bookmarked') === '1' && isAuthenticated) {
          setShowBookmarks(true);
          loadBookmarkedPlaces();
        } else {
          parseAndSearch(keyword);
        }
      })
      .catch(() => setSdkError(true));
    return () => {
      cancelled = true;
    };
  }, []);

  async function loadDefaultPlaces({ keepView = false } = {}) {
    const seq = ++searchSeqRef.current;
    const res = await getPlaces({ page: 0, size: 50 });
    if (seq !== searchSeqRef.current) return;
    setPage(0);
    setHasMore(!res.data.data.last);
    applyPlaces(res.data.data.content, { fitToKorea: !keepView, keepView });
    loadMoreRef.current = async (nextPage) => {
      const moreSeq = ++searchSeqRef.current;
      const moreRes = await getPlaces({ page: nextPage, size: 50 });
      if (moreSeq !== searchSeqRef.current) return;
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

  async function loadBookmarkedPlaces() {
    const seq = ++searchSeqRef.current;
    const res = await getMyBookmarkedPlaces({ page: 0, size: 50 });
    if (seq !== searchSeqRef.current) return;
    setPage(0);
    setHasMore(!res.data.data.last);
    applyPlaces(res.data.data.content, { fitToKorea: true });
    loadMoreRef.current = async (nextPage) => {
      const moreSeq = ++searchSeqRef.current;
      const moreRes = await getMyBookmarkedPlaces({ page: nextPage, size: 50 });
      if (moreSeq !== searchSeqRef.current) return;
      setPage(nextPage);
      setHasMore(!moreRes.data.data.last);
      applyPlaces(moreRes.data.data.content, { append: true, fitToKorea: true });
    };
  }

  function handleBookmarkToggle() {
    if (showBookmarks) {
      setShowBookmarks(false);
      loadDefaultPlaces();
      return;
    }
    if (!isAuthenticated) {
      showToast('로그인이 필요합니다.', 'error');
      return;
    }
    setShowRatingRanking(false);
    setShowBookmarks(true);
    loadBookmarkedPlaces();
  }

  async function loadRatingRankedPlaces() {
    const seq = ++searchSeqRef.current;
    const res = await getPlacesRankedByRating({ page: 0, size: 50 });
    if (seq !== searchSeqRef.current) return;
    setPage(0);
    setHasMore(!res.data.data.last);
    applyPlaces(res.data.data.content, { fitToKorea: true });
    loadMoreRef.current = async (nextPage) => {
      const moreSeq = ++searchSeqRef.current;
      const moreRes = await getPlacesRankedByRating({ page: nextPage, size: 50 });
      if (moreSeq !== searchSeqRef.current) return;
      setPage(nextPage);
      setHasMore(!moreRes.data.data.last);
      applyPlaces(moreRes.data.data.content, { append: true, fitToKorea: true });
    };
  }

  function handleRatingRankingToggle() {
    if (showRatingRanking) {
      setShowRatingRanking(false);
      loadDefaultPlaces();
      return;
    }
    setShowBookmarks(false);
    setShowRatingRanking(true);
    loadRatingRankedPlaces();
  }

  function applyPlaces(placeList, { append = false, fitToKorea = false, keepView = false } = {}) {
    setPlaces((prev) => (append ? [...prev, ...placeList] : placeList));
    loadRatingSummaries(placeList);
    if (!mapRef.current || !clustererRef.current) return;
    const { kakao, map } = mapRef.current;

    if (!append) {
      clustererRef.current.clear();
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
      const marker = new kakao.maps.Marker({ position, image });
      markerByIdRef.current.set(place.placeId, { marker, image });
      kakao.maps.event.addListener(marker, 'click', () => {
        if (activeIdRef.current === place.placeId) {
          clearSelection();
          return;
        }
        setActiveId(place.placeId);
        setSelectedPlaceId(null);
        highlightMarker(place.placeId);
        showInfoOverlay(place);
        scrollListToPlace(place.placeId);
      });
      return marker;
    });
    clustererRef.current.addMarkers(newMarkers);
    markersRef.current = [...markersRef.current, ...newMarkers];

    if (keepView) {
    } else if (fitToKorea) {
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
    wrapper.onclick = (e) => {
      e.stopPropagation();
      focusPlace(place);
      setSelectedPlaceId(place.placeId);
    };

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
    setSelectedPlaceId(null);
    highlightMarker(null);
    hideInfoOverlay();
  }

  function focusPlace(place) {
    setActiveId(place.placeId);
    setSelectedPlaceId(null);
    if (!mapRef.current || place.latitude == null || place.longitude == null) return;
    const { kakao, map } = mapRef.current;
    highlightMarker(place.placeId);
    showInfoOverlay(place);
    const position = new kakao.maps.LatLng(place.latitude, place.longitude);
    skipNextIdleRef.current = true;
    map.setLevel(5, { anchor: position });
    map.setCenter(position);
  }

  async function runSearch({ keyword: kw = keyword, region: rg = region, categoryKey = category, keepView = false } = {}) {
    setRadiusMode(null);
    setShowBookmarks(false);
    setShowRatingRanking(false);
    const selected = CATEGORIES.find((c) => c.key === categoryKey);
    if (!selectMode) {
      const nextParams = {};
      if (kw.trim()) nextParams.keyword = kw.trim();
      if (rg.trim()) nextParams.region = rg.trim();
      if (categoryKey) nextParams.category = categoryKey;
      setSearchParams(nextParams, { replace: true });
    }
    if (!kw.trim() && !rg.trim() && !selected?.cat1?.length) {
      loadDefaultPlaces({ keepView });
      return;
    }
    const seq = ++searchSeqRef.current;
    const res = await searchPlaces({
      keyword: kw || undefined,
      region: rg || undefined,
      cat1: toCat1Param(selected),
      cat2: selected?.cat2 || undefined,
      page: 0,
      size: 50,
    });
    if (seq !== searchSeqRef.current) return;
    setPage(0);
    setHasMore(!res.data.data.last);
    applyPlaces(res.data.data.content, { keepView });
    loadMoreRef.current = async (nextPage) => {
      const moreSeq = ++searchSeqRef.current;
      const moreRes = await searchPlaces({
        keyword: kw || undefined,
        region: rg || undefined,
        cat1: toCat1Param(selected),
        cat2: selected?.cat2 || undefined,
        page: nextPage,
        size: 50,
      });
      if (moreSeq !== searchSeqRef.current) return;
      setPage(nextPage);
      setHasMore(!moreRes.data.data.last);
      applyPlaces(moreRes.data.data.content, { append: true, keepView: true });
    };
  }

  function parseAndSearch(rawKeyword) {
    const parsed = parseSearchKeyword(rawKeyword, { regions: REGIONS, categories: CATEGORIES });
    const effectiveRegion = parsed.region || region;
    const effectiveCategoryKey = parsed.categoryKey || category;
    if (parsed.region) setRegion(parsed.region);
    if (parsed.categoryKey) setCategory(parsed.categoryKey);
    setKeyword(parsed.keyword);
    runSearch({ keyword: parsed.keyword, region: effectiveRegion, categoryKey: effectiveCategoryKey });
  }

  function handleSearchSubmit(e) {
    e.preventDefault();
    setIsKeywordSuggestOpen(false);
    parseAndSearch(keyword);
  }

  function handleRegionSelect(value) {
    setRegion(value);
    setIsRegionOpen(false);
    runSearch({ region: value });
  }

  function handleCategoryClick(key) {
    setCategory(key);
    if (radiusMode === 'area' && mapRef.current) {
      const { lat, lng, radius } = currentMapRadius();
      searchByRadius(lat, lng, radius, key);
      return;
    }
    if (radiusMode === 'nearby' && nearbyCoordsRef.current) {
      const { lat, lng } = nearbyCoordsRef.current;
      searchByRadius(lat, lng, nearbyRadius, key);
      return;
    }
    runSearch({ categoryKey: key });
  }

  function currentMapRadius() {
    const { map } = mapRef.current;
    const center = map.getCenter();
    const northEast = map.getBounds().getNorthEast();
    const halfHeight = distanceMeters(center.getLat(), center.getLng(), northEast.getLat(), center.getLng());
    const halfWidth = distanceMeters(center.getLat(), center.getLng(), center.getLat(), northEast.getLng());
    return {
      lat: center.getLat(),
      lng: center.getLng(),
      radius: Math.min(halfHeight, halfWidth),
    };
  }

  async function searchByRadius(lat, lng, radius, categoryKey) {
    setShowBookmarks(false);
    setShowRatingRanking(false);
    const seq = ++searchSeqRef.current;
    const selected = CATEGORIES.find((c) => c.key === categoryKey);
    const res = await searchPlaces({
      lat,
      lng,
      radius,
      cat1: toCat1Param(selected),
      cat2: selected?.cat2 || undefined,
      page: 0,
      size: 50,
    });
    if (seq !== searchSeqRef.current) return;
    setPage(0);
    setHasMore(!res.data.data.last);
    applyPlaces(res.data.data.content, { keepView: true });
    loadMoreRef.current = async (nextPage) => {
      const moreSeq = ++searchSeqRef.current;
      const moreRes = await searchPlaces({
        lat,
        lng,
        radius,
        cat1: toCat1Param(selected),
        cat2: selected?.cat2 || undefined,
        page: nextPage,
        size: 50,
      });
      if (moreSeq !== searchSeqRef.current) return;
      setPage(nextPage);
      setHasMore(!moreRes.data.data.last);
      applyPlaces(moreRes.data.data.content, { append: true, keepView: true });
    };
  }

  async function handleSearchThisArea() {
    if (!mapRef.current) return;
    if (radiusMode === 'area') {
      runSearch({ keepView: true });
      return;
    }
    setRadiusMode('area');
    const { lat, lng, radius } = currentMapRadius();
    await searchByRadius(lat, lng, radius, category);
  }

  function handleNearbySearch() {
    if (radiusMode === 'nearby') {
      runSearch({ keepView: true });
      return;
    }
    if (!navigator.geolocation) {
      showToast('이 브라우저는 위치 정보를 지원하지 않습니다.', 'error');
      return;
    }
    navigator.geolocation.getCurrentPosition(async (position) => {
      const { latitude, longitude } = position.coords;
      nearbyCoordsRef.current = { lat: latitude, lng: longitude };
      setRadiusMode('nearby');
      if (mapRef.current) {
        const { kakao, map } = mapRef.current;
        const position = new kakao.maps.LatLng(latitude, longitude);
        if (myLocationRef.current) {
          myLocationRef.current.marker.setMap(null);
          myLocationRef.current.circle.setMap(null);
        }
        const marker = new kakao.maps.Marker({
          position,
          map,
          image: new kakao.maps.MarkerImage(MY_LOCATION_MARKER_SVG, new kakao.maps.Size(24, 24), {
            offset: new kakao.maps.Point(12, 12),
          }),
          zIndex: 20,
        });
        const circle = new kakao.maps.Circle({
          center: position,
          radius: nearbyRadius,
          strokeWeight: 2,
          strokeColor: '#ff8a3d',
          strokeOpacity: 0.7,
          strokeStyle: 'shortdash',
          fillColor: '#ff8a3d',
          fillOpacity: 0.08,
        });
        circle.setMap(map);
        myLocationRef.current = { marker, circle };
        map.setCenter(position);
        map.setLevel(NEARBY_RADIUS_OPTIONS.find((o) => o.value === nearbyRadius)?.level ?? 5);
      }
      await searchByRadius(latitude, longitude, nearbyRadius, category);
    }, (error) => {
      const message =
        error.code === error.PERMISSION_DENIED
          ? '위치 권한이 거부되어 있습니다. 브라우저 주소창의 위치 권한을 허용해주세요.'
          : '현재 위치를 가져오지 못했습니다. 기기의 위치 서비스를 확인해주세요.';
      showToast(message, 'error');
    }, {
      timeout: 10000,
      maximumAge: 600000,
    });
  }

  function handleNearbyRadiusChange(value) {
    setNearbyRadius(value);
    const coords = nearbyCoordsRef.current;
    if (radiusMode !== 'nearby' || !coords) return;
    if (myLocationRef.current) {
      myLocationRef.current.circle.setRadius(value);
    }
    searchByRadius(coords.lat, coords.lng, value, category);
  }

  return (
    <div className={selectMode ? `${styles.page} ${styles.pageEmbedded}` : styles.page}>
      <form onSubmit={handleSearchSubmit} className={styles.searchBar}>
        <span className={styles.searchIcon}><MagnifyingGlassIcon /></span>
        <div className={styles.keywordField} ref={keywordSuggestRef}>
          <input
            type="text"
            placeholder="어디로 떠나시나요?"
            value={keyword}
            onChange={(e) => {
              setKeyword(e.target.value);
              setIsKeywordSuggestOpen(true);
            }}
            onFocus={() => setIsKeywordSuggestOpen(true)}
            onKeyDown={handleKeywordKeyDown}
          />
          {isKeywordSuggestOpen && keywordSuggestions.length > 0 && (
            <ul className={styles.suggestList}>
              {keywordSuggestions.map((suggestion, index) => (
                <li
                  key={`${suggestion.type}-${suggestion.value}`}
                  ref={(el) => {
                    if (el) suggestItemRefs.current.set(index, el);
                    else suggestItemRefs.current.delete(index);
                  }}
                >
                  <button
                    type="button"
                    className={index === highlightedIndex ? styles.suggestHighlighted : undefined}
                    onClick={() => handleSuggestionSelect(suggestion)}
                    onMouseEnter={() => setHighlightedIndex(index)}
                  >
                    <span className={styles.suggestLabel}>
                      {suggestion.label}
                      {suggestion.type === 'place' && suggestion.address && (
                        <span className={styles.suggestAddress}>{suggestion.address}</span>
                      )}
                    </span>
                    <span className={styles.suggestType}>
                      {suggestion.type === 'region' ? '지역' : suggestion.type === 'category' ? '카테고리' : '장소'}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
        <span className={styles.divider} />
        <div className={styles.regionDropdown} ref={regionDropdownRef}>
          <button
            type="button"
            className={styles.regionInput}
            onClick={() => setIsRegionOpen((prev) => !prev)}
          >
            {region || '지역 전체'}
          </button>
          {isRegionOpen && (
            <ul className={styles.regionMenu}>
              <li>
                <button type="button" onClick={() => handleRegionSelect('')}>
                  지역 전체
                </button>
              </li>
              {REGIONS.map((r) => (
                <li key={r}>
                  <button type="button" onClick={() => handleRegionSelect(r)}>
                    {r}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
        <div className={styles.searchActions}>
          <Button type="submit">검색</Button>
          <Button
            type="button"
            variant={radiusMode === 'area' ? 'primary' : 'secondary'}
            onClick={handleSearchThisArea}
          >
            <MapIcon /> 이 지역에서 검색
          </Button>
          <Button
            type="button"
            variant={radiusMode === 'nearby' ? 'primary' : 'secondary'}
            onClick={handleNearbySearch}
          >
            <MapPinIcon /> 내 주변
          </Button>
        </div>
      </form>

      <div className={styles.categoryChips}>
        {CATEGORIES.map((c) => (
          <button
            key={c.key}
            className={category === c.key && !showBookmarks && !showRatingRanking ? styles.chipActive : styles.chip}
            onClick={() => handleCategoryClick(c.key)}
          >
            {c.label}
          </button>
        ))}
        <button
          type="button"
          className={showBookmarks ? styles.chipActive : styles.chip}
          onClick={handleBookmarkToggle}
        >
          찜한곳
        </button>
        <button
          type="button"
          className={showRatingRanking ? styles.chipActive : styles.chip}
          onClick={handleRatingRankingToggle}
        >
          평점순
        </button>
      </div>

      {radiusMode === 'nearby' && (
        <div className={styles.categoryChips}>
          <span className={styles.radiusLabel}>검색 반경</span>
          {NEARBY_RADIUS_OPTIONS.map((option) => (
            <button
              key={option.value}
              type="button"
              className={nearbyRadius === option.value ? styles.chipActive : styles.chip}
              onClick={() => handleNearbyRadiusChange(option.value)}
            >
              {option.label}
            </button>
          ))}
        </div>
      )}

      <div className={styles.wrapper}>
        <aside className={styles.listPane} ref={listPaneRef}>
          <p className={styles.resultCount}>
            {places.length}곳의 {showBookmarks ? '찜한 장소' : showRatingRanking ? '평점순 장소' : '장소'}
          </p>
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
                    <span
                      className={styles.detailLink}
                      onClick={(e) => {
                        e.stopPropagation();
                        focusPlace(place);
                        setSelectedPlaceId(place.placeId);
                      }}
                    >
                      자세히 보기 →
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
            {places.length === 0 && (
              <p className={styles.empty}>{showBookmarks ? '찜한 장소가 없습니다.' : '검색 결과가 없습니다.'}</p>
            )}
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
          {selectedPlaceId && (
            <div className={styles.detailOverlay}>
              <PlaceDetailContent placeId={selectedPlaceId} onClose={() => setSelectedPlaceId(null)} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
