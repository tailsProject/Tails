// 장소 카테고리 필터, 라벨, 아이콘 매핑
export const CATEGORIES = [
  { key: '', label: '전체', cat1: [], cat2: '' },
  { key: 'cafe', label: '카페', cat1: ['FD'], cat2: 'FD05' },
  { key: 'restaurant', label: '식당', cat1: ['FD'], cat2: 'FD01' },
  { key: 'nature', label: '자연', cat1: ['NA'], cat2: '' },
  { key: 'shopping', label: '쇼핑', cat1: ['SH'], cat2: '' },
  { key: 'lodging', label: '숙박', cat1: ['AC'], cat2: '' },
  { key: 'attraction', label: '관광지', cat1: ['VE', 'HS'], cat2: '' },
  { key: 'sports', label: '레포츠', cat1: ['LS', 'EX'], cat2: '' },
  { key: 'etc', label: '기타', cat1: ['EV'], cat2: '' },
];

export function toCat1Param(categoryEntry) {
  return categoryEntry?.cat1?.length ? categoryEntry.cat1.join(',') : undefined;
}

const CATEGORY_LABELS_BY_CAT2 = {
  FD05: '카페',
  FD01: '식당',
  NA04: '공원',
};
const CATEGORY_LABELS_BY_CAT1 = {
  FD: '음식점',
  NA: '자연',
  HS: '역사',
  VE: '관광지',
  SH: '쇼핑',
  AC: '숙박',
  EX: '체험',
  LS: '레포츠',
  EV: '행사·공연·축제',
};
const CATEGORY_LABELS_BY_CONTENT_TYPE = {
  12: '관광지',
  14: '문화시설',
  15: '행사·공연·축제',
  28: '레포츠',
  32: '숙박',
  38: '쇼핑',
  39: '음식점',
};

export function getCategoryLabel(place) {
  return (
    CATEGORY_LABELS_BY_CAT2[place.cat2] ??
    CATEGORY_LABELS_BY_CAT1[place.cat1] ??
    CATEGORY_LABELS_BY_CONTENT_TYPE[place.contentTypeId] ??
    null
  );
}

const CATEGORY_ICON_BY_CAT2 = {
  FD05: 'category-cafe',
  FD01: 'category-restaurant',
  NA04: 'category-park',
};
const CATEGORY_ICON_BY_CAT1 = {
  FD: 'category-restaurant',
  NA: 'category-park',
  HS: 'category-attraction',
  VE: 'category-attraction',
  SH: 'category-shopping',
  AC: 'category-lodging',
  EX: 'category-attraction',
  LS: 'category-sports',
  EV: 'category-attraction',
};
const CATEGORY_ICON_BY_CONTENT_TYPE = {
  12: 'category-attraction',
  14: 'category-attraction',
  15: 'category-attraction',
  28: 'category-sports',
  32: 'category-lodging',
  38: 'category-shopping',
  39: 'category-restaurant',
};

export function getCategoryIconUrl(place) {
  const key =
    CATEGORY_ICON_BY_CAT2[place.cat2] ??
    CATEGORY_ICON_BY_CAT1[place.cat1] ??
    CATEGORY_ICON_BY_CONTENT_TYPE[place.contentTypeId] ??
    'category-attraction';
  return `/category-icons/${key}.png`;
}
