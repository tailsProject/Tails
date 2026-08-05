// 하루 단위 방문지 일정, 지도 표시와 순서 편집 담당
import { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  getTravelDetails,
  addTravelDetail,
  updateTravelDetail,
  deleteTravelDetail,
  reorderTravelDetails,
  optimizeRoute,
} from './api';
import { useToast } from '../../hooks/useToast';
import { useConfirm } from '../../hooks/useConfirm';
import Button from '../../components/Button/Button';
import { loadKakaoMaps } from '../place/kakaoLoader';
import { resolveImage } from '../../utils/resolveImage';
import PlaceSelectModal from './PlaceSelectModal';
import PlaceDetailModal from './PlaceDetailModal';
import { PawIcon, MapPinIcon, MapIcon, PlusIcon, PencilIcon, CheckIcon, XMarkIcon } from '../../components/Icon/Icon';
import styles from './DaySchedule.module.scss';

const EARTH_RADIUS_METERS = 6371000;
const STOP_COLORS = ['#ff8a3d', '#2f9e8f', '#7c6bdb', '#e5722a', '#1d8fc4', '#e04060'];
const DEFAULT_CENTER = { lat: 35.8714, lng: 128.6014 };

function distanceMeters(a, b) {
  const toRad = (deg) => (deg * Math.PI) / 180;
  const dLat = toRad(b.latitude - a.latitude);
  const dLng = toRad(b.longitude - a.longitude);
  const lat1 = toRad(a.latitude);
  const lat2 = toRad(b.latitude);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
  return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(h));
}

function formatDistance(meters) {
  return meters >= 1000 ? `${(meters / 1000).toFixed(1)}km` : `${Math.round(meters)}m`;
}

function formatVisitTime(time) {
  return time ? time.slice(0, 5) : time;
}

export default function DaySchedule({ travelId, date }) {
  const { showToast } = useToast();
  const confirm = useConfirm();
  const [details, setDetails] = useState([]);
  const [suggestion, setSuggestion] = useState(null); 
  const [editMode, setEditMode] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [editVisitTime, setEditVisitTime] = useState('');
  const [editMemo, setEditMemo] = useState('');
  const [pendingEdits, setPendingEdits] = useState({});
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [detailModalPlaceId, setDetailModalPlaceId] = useState(null);
  const [mapReady, setMapReady] = useState(false);

  const dragIndexRef = useRef(null);
  const mapContainerRef = useRef(null);
  const mapRef = useRef(null); 
  const visitOverlaysRef = useRef([]);
  const polylineRef = useRef(null);

  async function load() {
    const res = await getTravelDetails(travelId, date);
    setDetails(res.data.data);
    setSuggestion(null);
  }

  const sortedDetails = useMemo(() => {
    return [...details].sort((a, b) => {
      if (a.visitTime && b.visitTime) return a.visitTime.localeCompare(b.visitTime);
      if (a.visitTime) return -1;
      if (b.visitTime) return 1;
      return a.sequence - b.sequence;
    });
  }, [details]);

  useEffect(() => {
    load();
  }, [travelId, date]);

  useEffect(() => {
    let cancelled = false;
    loadKakaoMaps().then((kakao) => {
      if (cancelled || !mapContainerRef.current) return;
      mapRef.current = {
        kakao,
        map: new kakao.maps.Map(mapContainerRef.current, {
          center: new kakao.maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng),
          level: 8,
        }),
      };
      setMapReady(true);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!mapRef.current) return;
    const { kakao, map } = mapRef.current;

    visitOverlaysRef.current.forEach((overlay) => overlay.setMap(null));
    visitOverlaysRef.current = [];
    if (polylineRef.current) {
      polylineRef.current.setMap(null);
      polylineRef.current = null;
    }

    const withCoords = sortedDetails
      .map((detail, index) => ({ detail, index }))
      .filter(({ detail }) => detail.placeLatitude != null && detail.placeLongitude != null);
    const path = [];
    withCoords.forEach(({ detail, index }) => {
      const position = new kakao.maps.LatLng(detail.placeLatitude, detail.placeLongitude);
      path.push(position);

      const content = document.createElement('div');
      content.className = styles.mapPin;
      content.style.backgroundColor = STOP_COLORS[index % STOP_COLORS.length];
      content.style.cursor = 'pointer';
      content.textContent = index + 1;
      content.addEventListener('click', () => setDetailModalPlaceId(detail.placeId));
      const overlay = new kakao.maps.CustomOverlay({ map, position, content, xAnchor: 0.5, yAnchor: 0.5 });
      visitOverlaysRef.current.push(overlay);
    });

    if (path.length > 1) {
      polylineRef.current = new kakao.maps.Polyline({
        map,
        path,
        strokeWeight: 3,
        strokeColor: '#ff8a3d',
        strokeOpacity: 0.8,
        strokeStyle: 'solid',
      });
    }

    if (withCoords.length > 0) {
      const bounds = new kakao.maps.LatLngBounds();
      path.forEach((position) => bounds.extend(position));
      map.setBounds(bounds);
    }
  }, [sortedDetails, mapReady]);

  async function handleAddPlace(place) {
    try {
      await addTravelDetail(travelId, { placeId: place.placeId, travelDate: date, visitTime: null, memo: null });
      await load();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '추가에 실패했습니다.', 'error');
    }
  }

  // 수정 모드에서는 항목을 클릭할 때마다 바로 저장하지 않고 임시로만 담아둠
  function commitPendingEdit(detailId) {
    setPendingEdits((prev) => ({ ...prev, [detailId]: { visitTime: editVisitTime || null, memo: editMemo || null } }));
  }

  async function toggleEditMode() {
    if (editMode) {
      const edits = editingId != null
        ? { ...pendingEdits, [editingId]: { visitTime: editVisitTime || null, memo: editMemo || null } }
        : pendingEdits;
      const entries = Object.entries(edits);
      if (entries.length > 0) {
        try {
          await Promise.all(entries.map(([detailId, values]) => updateTravelDetail(travelId, detailId, values)));
        } catch (error) {
          showToast(error.response?.data?.error?.message ?? '수정에 실패했습니다.', 'error');
          return;
        }
        load();
      }
      setPendingEdits({});
      setEditingId(null);
      setEditMode(false);
    } else {
      setEditMode(true);
    }
  }

  function openEdit(detail) {
    if (editingId != null && editingId !== detail.detailId) {
      commitPendingEdit(editingId);
    }
    const pending = pendingEdits[detail.detailId];
    setEditingId(detail.detailId);
    setEditVisitTime(formatVisitTime(pending ? pending.visitTime : detail.visitTime) ?? '');
    setEditMemo((pending ? pending.memo : detail.memo) ?? '');
  }

  async function handleDelete(detailId) {
    const ok = await confirm('이 방문지를 일정에서 삭제하시겠습니까?');
    if (!ok) return;
    if (editingId === detailId) setEditingId(null);
    setPendingEdits((prev) => {
      if (!(detailId in prev)) return prev;
      const next = { ...prev };
      delete next[detailId];
      return next;
    });
    try {
      await deleteTravelDetail(travelId, detailId);
      load();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '삭제에 실패했습니다.', 'error');
    }
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

  async function handleOptimize() {
    try {
      const res = await optimizeRoute(travelId, date);
      setSuggestion(res.data.data);
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '경로 계산에 실패했습니다.', 'error');
    }
  }

  // 추천 순서를 적용하면 기존 방문 시간과 순서가 맞지 않을 수 있어 시간 설정된 방문지는 초기화
  async function handleApplySuggestion() {
    const timedDetails = suggestion.orderedDetails.filter((d) => d.visitTime);
    if (timedDetails.length > 0) {
      const ok = await confirm('시간이 설정된 방문지가 있습니다. 순서를 반영하면 해당 시간이 초기화됩니다. 계속할까요?');
      if (!ok) return;
    }

    try {
      await reorderTravelDetails(travelId, {
        travelDate: date,
        detailIds: suggestion.orderedDetails.map((d) => d.detailId),
      });
      await Promise.all(
        timedDetails.map((d) => updateTravelDetail(travelId, d.detailId, { visitTime: null, memo: d.memo })),
      );
      setSuggestion(null);
      load();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '순서 반영에 실패했습니다.', 'error');
    }
  }

  const totalDistanceMeters = sortedDetails.reduce((sum, detail, index) => {
    const next = sortedDetails[index + 1];
    if (!next || detail.placeLatitude == null || detail.placeLongitude == null || next.placeLatitude == null || next.placeLongitude == null) {
      return sum;
    }
    return sum + distanceMeters(
      { latitude: detail.placeLatitude, longitude: detail.placeLongitude },
      { latitude: next.placeLatitude, longitude: next.placeLongitude },
    );
  }, 0);
  const addedPlaceIds = new Set(details.map((d) => d.placeId));

  return (
    <div className={styles.wrapper}>
      <div className={styles.toolbar}>
        <Button variant="secondary" onClick={() => setAddModalOpen(true)} className={styles.addPlaceBtn}>
          <PlusIcon /> 방문지 추가
        </Button>
        {details.length > 1 && (
          <Button variant="secondary" onClick={handleOptimize} className={styles.optimizeBtn}>
            <MapIcon /> 자동 정렬
          </Button>
        )}
        {details.length > 0 && (
          <Button
            variant={editMode ? 'primary' : 'secondary'}
            onClick={toggleEditMode}
            className={styles.editModeBtn}
          >
            {editMode ? (
              <>
                <CheckIcon /> 저장
              </>
            ) : (
              <>
                <PencilIcon /> 수정
              </>
            )}
          </Button>
        )}
      </div>

      <div className={styles.layout}>
        <div className={styles.listCol}>
        {suggestion && (
          <div className={styles.suggestionBox}>
            <p>
              추천 순서 적용 시 총 이동거리 약 {Math.round(suggestion.totalDistanceMeters)}m —{' '}
              {suggestion.orderedDetails.map((d) => d.placeName).join(' → ')}
            </p>
            <div className={styles.suggestionActions}>
              <Button onClick={handleApplySuggestion}>이 순서로 반영</Button>
              <Button variant="secondary" onClick={() => setSuggestion(null)}>
                취소
              </Button>
            </div>
          </div>
        )}

        <ul className={styles.list}>
          {sortedDetails.map((detail, index) => {
            const next = sortedDetails[index + 1];
            const canMeasure =
              next && detail.placeLatitude != null && detail.placeLongitude != null &&
              next.placeLatitude != null && next.placeLongitude != null;
            const pending = pendingEdits[detail.detailId];
            const displayVisitTime = pending ? pending.visitTime : detail.visitTime;
            const displayMemo = pending ? pending.memo : detail.memo;

            return (
              <li key={detail.detailId}>
                <div
                  className={styles.item}
                  draggable={!editMode}
                  onDragStart={() => handleDragStart(index)}
                  onDragOver={handleDragOver}
                  onDrop={() => handleDrop(index)}
                >
                  <div className={styles.timelineCol}>
                    <span
                      className={styles.sequence}
                      style={{ backgroundColor: STOP_COLORS[index % STOP_COLORS.length] }}
                    >
                      {index + 1}
                    </span>
                    {index < sortedDetails.length - 1 && <span className={styles.timelineLine} />}
                  </div>
                  <div
                    className={editMode ? `${styles.itemCard} ${styles.itemCardEditable}` : styles.itemCard}
                    onClick={() => editMode && openEdit(detail)}
                  >
                    <div className={styles.itemThumb}>
                      {resolveImage(detail.placeImageUrl) ? (
                        <img src={resolveImage(detail.placeImageUrl)} alt="" />
                      ) : (
                        <span><PawIcon /></span>
                      )}
                    </div>
                    <div className={styles.itemBody}>
                      <Link
                        to={`/places/${detail.placeId}`}
                        className={styles.placeName}
                        onClick={(e) => {
                          e.preventDefault();
                          if (editMode) {
                            openEdit(detail);
                          } else {
                            setDetailModalPlaceId(detail.placeId);
                          }
                        }}
                      >
                        {detail.placeName}
                      </Link>
                      {editingId === detail.detailId ? (
                        <div className={styles.editRow} onClick={(e) => e.stopPropagation()}>
                          <input
                            type="time"
                            value={editVisitTime}
                            onChange={(e) => setEditVisitTime(e.target.value)}
                          />
                          <input
                            type="text"
                            placeholder="메모"
                            value={editMemo}
                            onChange={(e) => setEditMemo(e.target.value)}
                          />
                        </div>
                      ) : (
                        <span className={styles.meta}>
                          {displayVisitTime && <span className={styles.visitTime}>{formatVisitTime(displayVisitTime)}</span>}
                          {displayMemo}
                        </span>
                      )}
                    </div>
                    {editMode && (
                      <button
                        type="button"
                        className={styles.itemDeleteBtn}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDelete(detail.detailId);
                        }}
                        aria-label="삭제"
                      >
                        <XMarkIcon />
                      </button>
                    )}
                  </div>
                </div>
                {canMeasure && (
                  <div className={styles.distanceRow}>
                    <span className={styles.distanceLine} />
                    {formatDistance(distanceMeters(
                      { latitude: detail.placeLatitude, longitude: detail.placeLongitude },
                      { latitude: next.placeLatitude, longitude: next.placeLongitude },
                    ))}
                  </div>
                )}
              </li>
            );
          })}
          {details.length === 0 && (
            <div className={styles.empty}>
              <p className={styles.emptyIcon}><MapPinIcon /></p>
              <p>이 날짜에 등록된 방문지가 없습니다.</p>
              <p className={styles.emptyHint}>방문지 추가 버튼을 눌러 지도에서 바로 검색해보세요.</p>
            </div>
          )}
        </ul>
      </div>

      <div className={styles.mapCol}>
        <div ref={mapContainerRef} className={styles.map} />

        {details.length > 0 && (
          <div className={styles.stats}>
            <div>
              <span className={styles.statValue}>{details.length}곳</span>
              <span className={styles.statLabel}>방문지</span>
            </div>
            <div>
              <span className={styles.statValue}>{formatDistance(totalDistanceMeters)}</span>
              <span className={styles.statLabel}>총 이동거리</span>
            </div>
          </div>
        )}
      </div>
      </div>

      <PlaceSelectModal
        open={addModalOpen}
        onClose={() => setAddModalOpen(false)}
        addedPlaceIds={addedPlaceIds}
        onAddPlace={handleAddPlace}
      />

      <PlaceDetailModal
        open={detailModalPlaceId != null}
        placeId={detailModalPlaceId}
        onClose={() => setDetailModalPlaceId(null)}
      />
    </div>
  );
}
