// 공유 링크로 비로그인 열람하는 여행 일정 페이지
import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getSharedTravel } from './api';
import StateMessage from '../../components/StateMessage/StateMessage';
import { resolveImage } from '../../utils/resolveImage';
import { loadKakaoMaps } from '../place/kakaoLoader';
import PlaceDetailModal from './PlaceDetailModal';
import { SuitcaseIcon, PawIcon, MapPinIcon } from '../../components/Icon/Icon';
import { dDayLabel, nightsLabel } from './travelUtils';
import heroStyles from './TravelDetailPage.module.scss';
import dayStyles from './DaySchedule.module.scss';

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];
const STOP_COLORS = ['#ff8a3d', '#2f9e8f', '#7c6bdb', '#e5722a', '#1d8fc4', '#e04060'];
const EARTH_RADIUS_METERS = 6371000;
const DEFAULT_CENTER = { lat: 35.8714, lng: 128.6014 };

function formatLocalDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function getDateRange(start, end) {
  const dates = [];
  const current = new Date(`${start}T00:00:00`);
  const last = new Date(`${end}T00:00:00`);
  while (current <= last) {
    dates.push(formatLocalDate(current));
    current.setDate(current.getDate() + 1);
  }
  return dates;
}

function formatVisitTime(time) {
  return time ? time.slice(0, 5) : time;
}

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

export default function SharedTravelPage() {
  const { shareToken } = useParams();
  const [travel, setTravel] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const [detailModalPlaceId, setDetailModalPlaceId] = useState(null);

  const [mapReady, setMapReady] = useState(false);
  const mapContainerRef = useRef(null);
  const mapRef = useRef(null);
  const visitOverlaysRef = useRef([]);
  const polylineRef = useRef(null);

  useEffect(() => {
    getSharedTravel(shareToken)
      .then((res) => {
        const data = res.data.data;
        setTravel(data);
        setSelectedDate(data.startDate);
      })
      .catch(() => setNotFound(true));
  }, [shareToken]);

  const detailsByDate = useMemo(() => {
    if (!travel) return {};
    return travel.details.reduce((acc, detail) => {
      (acc[detail.travelDate] ??= []).push(detail);
      return acc;
    }, {});
  }, [travel]);

  const sortedDetails = useMemo(() => {
    const details = detailsByDate[selectedDate] ?? [];
    return [...details].sort((a, b) => {
      if (a.visitTime && b.visitTime) return a.visitTime.localeCompare(b.visitTime);
      if (a.visitTime) return -1;
      if (b.visitTime) return 1;
      return a.sequence - b.sequence;
    });
  }, [detailsByDate, selectedDate]);

  useEffect(() => {
    if (!travel || mapRef.current) return;
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
  }, [travel]);

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
      content.className = dayStyles.mapPin;
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

  if (notFound) {
    return (
      <StateMessage
        icon={SuitcaseIcon}
        title="공유 링크를 찾을 수 없어요"
        description="공유가 중단되었거나 존재하지 않는 링크예요."
        actionTo="/"
        actionLabel="홈으로 가기"
      />
    );
  }

  if (!travel) {
    return null;
  }

  const dates = getDateRange(travel.startDate, travel.endDate);
  const dday = dDayLabel(travel.startDate, travel.endDate);
  const cover = resolveImage(travel.thumbnailUrl);

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

  return (
    <div className={heroStyles.wrapper}>
      <div className={heroStyles.hero} style={cover ? { backgroundImage: `url(${cover})` } : undefined}>
        {!cover && <span className={heroStyles.heroIcon}><SuitcaseIcon /></span>}
        <div className={heroStyles.heroOverlay} />

        <div className={heroStyles.heroContent}>
          <div className={heroStyles.badgeRow}>
            <span className={`${heroStyles.ddayBadge} ${heroStyles[dday.tone]}`}>{dday.text}</span>
            <span className={heroStyles.nightsLabel}>{nightsLabel(travel.startDate, travel.endDate)}</span>
          </div>
          <h1>{travel.title}</h1>
          <p className={heroStyles.dates}>
            {travel.startDate} ~ {travel.endDate}
          </p>
          {travel.description && <p className={heroStyles.description}>{travel.description}</p>}
          {travel.pets.length > 0 && (
            <div className={heroStyles.petRow}>
              {travel.pets.map((pet) => (
                <span key={pet.petId} className={heroStyles.petChip}>
                  {resolveImage(pet.photoImg) ? (
                    <img src={resolveImage(pet.photoImg)} alt="" className={heroStyles.petChipPhoto} />
                  ) : (
                    <PawIcon />
                  )}
                  {pet.name}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className={heroStyles.dayTabsWrap}>
        <div className={heroStyles.dayTabs}>
          {dates.map((date, index) => {
            const weekday = WEEKDAYS[new Date(`${date}T00:00:00`).getDay()];
            const [, month, day] = date.split('-');
            return (
              <button
                key={date}
                type="button"
                className={date === selectedDate ? heroStyles.dayActive : heroStyles.day}
                onClick={() => setSelectedDate(date)}
              >
                <span className={heroStyles.dayNum}>DAY {index + 1}</span>
                <span className={heroStyles.dayDate}>
                  {Number(month)}.{Number(day)} <em>{weekday}</em>
                </span>
              </button>
            );
          })}
        </div>
      </div>

      <div className={dayStyles.wrapper}>
        <div className={dayStyles.layout}>
          <div className={dayStyles.listCol}>
            <ul className={dayStyles.list}>
              {sortedDetails.map((detail, index) => {
                const next = sortedDetails[index + 1];
                const canMeasure =
                  next && detail.placeLatitude != null && detail.placeLongitude != null &&
                  next.placeLatitude != null && next.placeLongitude != null;

                return (
                  <li key={detail.detailId}>
                    <div className={dayStyles.item}>
                      <div className={dayStyles.timelineCol}>
                        <span
                          className={dayStyles.sequence}
                          style={{ backgroundColor: STOP_COLORS[index % STOP_COLORS.length] }}
                        >
                          {index + 1}
                        </span>
                        {index < sortedDetails.length - 1 && <span className={dayStyles.timelineLine} />}
                      </div>
                      <div className={dayStyles.itemCard}>
                        <div className={dayStyles.itemThumb}>
                          {resolveImage(detail.placeImageUrl) ? (
                            <img src={resolveImage(detail.placeImageUrl)} alt="" />
                          ) : (
                            <span><PawIcon /></span>
                          )}
                        </div>
                        <div className={dayStyles.itemBody}>
                          <Link
                            to={`/places/${detail.placeId}`}
                            className={dayStyles.placeName}
                            onClick={(e) => {
                              e.preventDefault();
                              setDetailModalPlaceId(detail.placeId);
                            }}
                          >
                            {detail.placeName}
                          </Link>
                          <span className={dayStyles.meta}>
                            {detail.visitTime && (
                              <span className={dayStyles.visitTime}>{formatVisitTime(detail.visitTime)}</span>
                            )}
                            {detail.memo}
                          </span>
                        </div>
                      </div>
                    </div>
                    {canMeasure && (
                      <div className={dayStyles.distanceRow}>
                        <span className={dayStyles.distanceLine} />
                        {formatDistance(distanceMeters(
                          { latitude: detail.placeLatitude, longitude: detail.placeLongitude },
                          { latitude: next.placeLatitude, longitude: next.placeLongitude },
                        ))}
                      </div>
                    )}
                  </li>
                );
              })}
              {sortedDetails.length === 0 && (
                <div className={dayStyles.empty}>
                  <p className={dayStyles.emptyIcon}><MapPinIcon /></p>
                  <p>이 날짜에 등록된 방문지가 없습니다.</p>
                </div>
              )}
            </ul>
          </div>

          <div className={dayStyles.mapCol}>
            <div ref={mapContainerRef} className={dayStyles.map} />

            {sortedDetails.length > 0 && (
              <div className={dayStyles.stats}>
                <div>
                  <span className={dayStyles.statValue}>{sortedDetails.length}곳</span>
                  <span className={dayStyles.statLabel}>방문지</span>
                </div>
                <div>
                  <span className={dayStyles.statValue}>{formatDistance(totalDistanceMeters)}</span>
                  <span className={dayStyles.statLabel}>총 이동거리</span>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      <PlaceDetailModal
        open={detailModalPlaceId != null}
        placeId={detailModalPlaceId}
        onClose={() => setDetailModalPlaceId(null)}
      />
    </div>
  );
}
