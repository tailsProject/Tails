// 장소 조회, 검색, 리뷰 관련 API 호출 모음
import client from '../../api/client';

export function getPlaces({ page = 0, size = 20 } = {}) {
  return client.get('/api/places', { params: { page, size } });
}

export function getPlaceDetail(placeId) {
  return client.get(`/api/places/${placeId}`);
}

export function searchPlaces({ keyword, cat1, cat2, region, lat, lng, radius, page = 0, size = 50 } = {}) {
  return client.get('/api/places/search', { params: { keyword, cat1, cat2, region, lat, lng, radius, page, size } });
}

export function getPlacesByCategory({ cat1, cat2 }) {
  return client.get('/api/places/category', { params: { cat1, cat2 } });
}

// keyword가 이름에 포함된 장소를 최대 8건 추천하는 검색창 자동완성
export function autocompletePlaces(keyword) {
  return client.get('/api/places/autocomplete', { params: { keyword } });
}

// 여러 장소의 평균 별점과 리뷰 수를 한 번에 조회
export function getPlaceRatingSummaries(placeIds) {
  return client.get('/api/reviews/rating-summary', { params: { placeIds: placeIds.join(',') } });
}

export function togglePlaceBookmark(placeId) {
  return client.post(`/api/places/${placeId}/bookmark`);
}

export function createReview(placeId, { rating, content }) {
  return client.post(`/api/places/${placeId}/reviews`, { rating, content });
}

export function getReviews(placeId, { page = 0, size = 10 } = {}) {
  return client.get(`/api/places/${placeId}/reviews`, { params: { page, size } });
}

export function updateReview(placeId, reviewId, { rating, content }) {
  return client.patch(`/api/places/${placeId}/reviews/${reviewId}`, { rating, content });
}

export function deleteReview(placeId, reviewId) {
  return client.delete(`/api/places/${placeId}/reviews/${reviewId}`);
}

export function uploadReviewImages(reviewId, files) {
  const formData = new FormData();
  files.forEach((file) => formData.append('files', file));
  return client.post(`/api/reviews/${reviewId}/images`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export function getReviewImages(reviewId) {
  return client.get(`/api/reviews/${reviewId}/images`);
}

export function deleteReviewImage(imageId) {
  return client.delete(`/api/images/${imageId}`);
}
