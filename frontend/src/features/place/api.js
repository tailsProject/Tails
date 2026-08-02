// 장소/리뷰 조회 API. Place 지도(박영준 트랙)가 아직 없어서 신고 버튼을 붙일 최소
// 뼈대 화면(ReviewSection)용으로 임시로 여기 둠. Place 프론트 구현 시 정리 필요.
import client from '../../api/client';

export function getPlaceReviews(placeId, { page = 0, size = 10 } = {}) {
  return client.get(`/api/places/${placeId}/reviews`, { params: { page, size } });
}

export function getPlaceDetail(placeId) {
  return client.get(`/api/places/${placeId}`);
}
