// 여행 일정, 세부 일정, 공유 링크 관련 API 호출 모음
import client from '../../api/client';

export function createTravel({ title, description, startDate, endDate, petIds }) {
  return client.post('/api/travels', { title, description, startDate, endDate, petIds });
}

export function getMyTravels({ page = 0, size = 10 } = {}) {
  return client.get('/api/travels', { params: { page, size } });
}

export function getTravelDetail(travelId) {
  return client.get(`/api/travels/${travelId}`);
}

export function updateTravel(travelId, { title, description, startDate, endDate, petIds }) {
  return client.put(`/api/travels/${travelId}`, { title, description, startDate, endDate, petIds });
}

export function deleteTravel(travelId) {
  return client.delete(`/api/travels/${travelId}`);
}

export function shareTravel(travelId) {
  return client.post(`/api/travels/${travelId}/share`);
}

export function unshareTravel(travelId) {
  return client.delete(`/api/travels/${travelId}/share`);
}

export function getSharedTravel(shareToken) {
  return client.get(`/api/travels/shared/${shareToken}`);
}

export function addTravelDetail(travelId, { placeId, travelDate, visitTime, memo }) {
  return client.post(`/api/travels/${travelId}/details`, { placeId, travelDate, visitTime, memo });
}

export function getTravelDetails(travelId, date) {
  return client.get(`/api/travels/${travelId}/details`, { params: { date } });
}

export function updateTravelDetail(travelId, detailId, { visitTime, memo }) {
  return client.put(`/api/travels/${travelId}/details/${detailId}`, { visitTime, memo });
}

export function deleteTravelDetail(travelId, detailId) {
  return client.delete(`/api/travels/${travelId}/details/${detailId}`);
}

export function reorderTravelDetails(travelId, { travelDate, detailIds }) {
  return client.patch(`/api/travels/${travelId}/details/order`, { travelDate, detailIds });
}

export function optimizeRoute(travelId, date) {
  return client.get(`/api/travels/${travelId}/details/optimize-route`, { params: { date } });
}
