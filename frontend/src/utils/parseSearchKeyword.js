// 지도 검색창 입력에서 지역/카테고리 단어를 분리하는 유틸
// 카테고리 키워드 확장 사전, 실제 카테고리명 외 표현도 함께 매칭
const CATEGORY_SYNONYMS = {
  cafe: ['카공', '스터디카페', '감성카페', '브런치카페', '디저트카페', '루프탑카페', '애견카페', '커피숍', '커피', '베이커리', '빵집', '케이크', '디저트'],
  restaurant: ['음식점', '음식', '맛집투어', '맛집', '먹거리', '맛있는집', '맛있는곳', '맛있는 곳', '고깃집', '고기집', '분식집', '술집'],
  nature: ['산책로', '산책', '애견운동장', '애견공원', '공원', '피크닉'],
  shopping: ['아울렛', '전통시장', '쇼핑몰', '백화점', '시장'],
  attraction: ['여행지', '여행', '핫플레이스', '핫플', '가볼만한곳', '가볼만한 곳', '명소', '포토존'],
  sports: ['운동', '스포츠', '액티비티', '등산', '트레킹'],
};

function findCategoryMatch(text, categories) {
  const candidates = [];
  for (const c of categories) {
    if (!c.key) continue;
    const words = [c.label, ...(CATEGORY_SYNONYMS[c.key] ?? [])];
    for (const word of words) {
      if (text.includes(word)) {
        candidates.push({ key: c.key, word });
      }
    }
  }
  if (candidates.length === 0) return null;
  candidates.sort((a, b) => b.word.length - a.word.length);
  return candidates[0];
}

// 정식 행정구역명과 줄임말 매핑
const REGION_ALIASES = {
  서울특별시: '서울',
  부산광역시: '부산',
  대구광역시: '대구',
  인천광역시: '인천',
  광주광역시: '광주',
  대전광역시: '대전',
  울산광역시: '울산',
  세종특별자치시: '세종',
  세종시: '세종',
  경기도: '경기',
  강원특별자치도: '강원',
  강원도: '강원',
  충청북도: '충북',
  충청남도: '충남',
  전북특별자치도: '전북',
  전라북도: '전북',
  전라남도: '전남',
  경상북도: '경북',
  경상남도: '경남',
  제주특별자치도: '제주',
  제주도: '제주',
};

function findRegionMatch(text, regions) {
  const candidates = regions.map((r) => ({ region: r, alias: r }));
  for (const [alias, region] of Object.entries(REGION_ALIASES)) {
    if (regions.includes(region)) {
      candidates.push({ region, alias });
    }
  }
  const matched = candidates.filter((c) => text.includes(c.alias));
  if (matched.length === 0) return null;
  matched.sort((a, b) => b.alias.length - a.alias.length);
  return matched[0];
}

export function parseSearchKeyword(input, { regions = [], categories = [] } = {}) {
  const original = (input ?? '').trim();
  let remaining = original;
  let region = null;
  let categoryKey = null;

  const regionMatch = findRegionMatch(remaining, regions);
  if (regionMatch) {
    region = regionMatch.region;
    remaining = remaining.replace(regionMatch.alias, '').trim();
  }

  const categoryMatch = findCategoryMatch(remaining, categories);
  if (categoryMatch) {
    categoryKey = categoryMatch.key;
    remaining = remaining.replace(categoryMatch.word, '').trim();
  }

  // 지역만 뽑히고 남은 글자가 있으면 지역명이 우연히 포함된 장소명 검색으로 간주, 원문 그대로 사용
  if (region && !categoryKey && remaining !== '') {
    return { keyword: original, region: null, categoryKey: null };
  }

  return { keyword: remaining, region, categoryKey };
}
