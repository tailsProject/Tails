// 여행 카드용 D-day, 박수 표시 계산 유틸
export function dDayLabel(startDate, endDate) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const start = new Date(`${startDate}T00:00:00`);
  const end = new Date(`${endDate}T00:00:00`);
  const diffToStart = Math.round((start - today) / (1000 * 60 * 60 * 24));
  const diffToEnd = Math.round((end - today) / (1000 * 60 * 60 * 24));

  if (diffToStart > 0) return { text: `D-${diffToStart}`, tone: 'upcoming' };
  if (diffToEnd >= 0) return { text: '여행중', tone: 'ongoing' };
  return { text: '다녀옴', tone: 'done' };
}

export function nightsLabel(startDate, endDate) {
  const start = new Date(`${startDate}T00:00:00`);
  const end = new Date(`${endDate}T00:00:00`);
  const nights = Math.round((end - start) / (1000 * 60 * 60 * 24));
  return nights > 0 ? `${nights}박${nights + 1}일` : '당일치기';
}
