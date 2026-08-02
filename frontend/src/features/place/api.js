// 장소 조회, 검색, 리뷰 관련 API 호출 모음
import client from '../../api/client';

export function getPlaces({ page = 0, size = 20 } = {}) {
  return client.get('/api/places', { params: { page, size } });
}

// 여러 장소의 평균 별점과 리뷰 수를 한 번에 조회
export function getPlaceRatingSummaries(placeIds) {
  return client.get('/api/reviews/rating-summary', { params: { placeIds: placeIds.join(',') } });
}
