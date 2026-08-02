// 공유 링크로 비로그인 열람하는 여행 일정 페이지
import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getSharedTravel } from './api';
import StateMessage from '../../components/StateMessage/StateMessage';
import { SuitcaseIcon, MapPinIcon } from '../../components/Icon/Icon';
import travelStyles from './TravelDetailPage.module.scss';
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

  return (
    <div className={travelStyles.wrapper}>
      <div className={travelStyles.header}>
        <div>
          <h1>{travel.title}</h1>
          <p className={travelStyles.dates}>
            {travel.startDate} ~ {travel.endDate}
          </p>
        </div>
      </div>

      <div className={travelStyles.dayTabs}>
        {dates.map((date, index) => {
          const weekday = WEEKDAYS[new Date(`${date}T00:00:00`).getDay()];
          const [, month, day] = date.split('-');
          return (
            <button
              key={date}
              type="button"
              className={date === selectedDate ? travelStyles.dayActive : travelStyles.day}
              onClick={() => setSelectedDate(date)}
            >
              <span className={travelStyles.dayNum}>DAY {index + 1}</span>
              <span className={travelStyles.dayDate}>
                {Number(month)}.{Number(day)} <em>{weekday}</em>
              </span>
            </button>
          );
        })}
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
