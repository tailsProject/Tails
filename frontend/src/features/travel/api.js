// 여행 일정, 세부 일정 관련 API 호출 모음
import client from '../../api/client';

export function createTravel({ title, startDate, endDate }) {
  return client.post('/api/travels', { title, startDate, endDate });
}

export function getMyTravels({ page = 0, size = 10 } = {}) {
  return client.get('/api/travels', { params: { page, size } });
}

export function getTravelDetail(travelId) {
  return client.get(`/api/travels/${travelId}`);
}

export function updateTravel(travelId, { title, startDate, endDate }) {
  return client.put(`/api/travels/${travelId}`, { title, startDate, endDate });
}

export function deleteTravel(travelId) {
  return client.delete(`/api/travels/${travelId}`);
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
