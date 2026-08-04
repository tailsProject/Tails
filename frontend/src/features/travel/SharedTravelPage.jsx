// 공유 링크로 비로그인 열람하는 여행 일정 페이지
import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getSharedTravel } from './api';
import StateMessage from '../../components/StateMessage/StateMessage';
import { resolveImage } from '../../utils/resolveImage';
import { SuitcaseIcon, PawIcon, MapPinIcon } from '../../components/Icon/Icon';
import { dDayLabel, nightsLabel } from './travelUtils';
import heroStyles from './TravelDetailPage.module.scss';
import dayStyles from './DaySchedule.module.scss';

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];

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

export default function SharedTravelPage() {
  const { shareToken } = useParams();
  const [travel, setTravel] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    getSharedTravel(shareToken)
      .then((res) => {
        const data = res.data.data;
        setTravel(data);
        setSelectedDate(data.startDate);
      })
      .catch(() => setNotFound(true));
  }, [shareToken]);

  const sortedDetails = useMemo(() => {
    if (!travel) return [];
    return travel.details
      .filter((detail) => detail.travelDate === selectedDate)
      .sort((a, b) => a.sequence - b.sequence);
  }, [travel, selectedDate]);

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

      <ul className={dayStyles.list}>
        {sortedDetails.map((detail, index) => (
          <li key={detail.detailId}>
            <div className={dayStyles.item}>
              <span className={dayStyles.itemIcon}>
                <MapPinIcon />
              </span>
              <div className={dayStyles.itemBody}>
                <p className={dayStyles.placeName}>
                  {index + 1}. {detail.placeName}
                </p>
              </div>
            </div>
          </li>
        ))}
        {sortedDetails.length === 0 && (
          <div className={dayStyles.empty}>
            <p className={dayStyles.emptyIcon}>
              <MapPinIcon />
            </p>
            <p>이 날짜에 등록된 방문지가 없습니다.</p>
          </div>
        )}
      </ul>
    </div>
  );
}
