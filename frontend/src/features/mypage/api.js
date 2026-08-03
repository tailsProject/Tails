import client from '../../api/client';

// 내 정보
export function getMyInfo() {
  return client.get('/api/members/me');
}

export function getMyStats() {
  return client.get('/api/members/me/stats');
}

export function updateMyInfo({ nickname, profileImg }) {
  return client.patch('/api/members/me', { nickname, profileImg });
}

export function changePassword({ currentPassword, newPassword, newPasswordConfirm }) {
  return client.patch('/api/members/me/password', { currentPassword, newPassword, newPasswordConfirm });
}

export function uploadProfileImage(file) {
  const formData = new FormData();
  formData.append('file', file);
  return client.post('/api/members/me/profile-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export function deleteProfileImage() {
  return client.delete('/api/members/me/profile-image');
}

export function withdraw() {
  return client.delete('/api/members/me');
}

// 반려동물
export function getMyPets() {
  return client.get('/api/pets');
}

export function createPet({ name, species, birthDate }) {
  return client.post('/api/pets', { name, species, birthDate });
}

export function updatePet(petId, { name, species, birthDate }) {
  return client.put(`/api/pets/${petId}`, { name, species, birthDate });
}

export function deletePet(petId) {
  return client.delete(`/api/pets/${petId}`);
}

export function uploadPetPhoto(petId, file) {
  const formData = new FormData();
  formData.append('file', file);
  return client.post(`/api/pets/${petId}/photo`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export function deletePetPhoto(petId) {
  return client.delete(`/api/pets/${petId}/photo`);
}

// 알림
export function getMyNotifications({ page = 0, size = 20 } = {}) {
  return client.get('/api/notifications', { params: { page, size } });
}

export function markNotificationAsRead(notificationId) {
  return client.patch(`/api/notifications/${notificationId}/read`);
}

export function markAllNotificationsAsRead() {
  return client.patch('/api/notifications/read-all');
}

export function updateFcmToken(fcmToken) {
  return client.patch('/api/members/me/fcm-token', { fcmToken });
}

// 내 글 / 내 리뷰 / 북마크
export function getMyBoards({ page = 0, size = 10 } = {}) {
  return client.get('/api/boards/my', { params: { page, size } });
}

export function getMyReviews({ page = 0, size = 10 } = {}) {
  return client.get('/api/reviews/me', { params: { page, size } });
}

export function getMyBookmarkedBoards({ page = 0, size = 10 } = {}) {
  return client.get('/api/bookmarks/boards', { params: { page, size } });
}

export function getMyBookmarkedPlaces({ page = 0, size = 10 } = {}) {
  return client.get('/api/bookmarks/places', { params: { page, size } });
}

// 내 신고 내역
export function getMyReports({ page = 0, size = 10 } = {}) {
  return client.get('/api/reports/my', { params: { page, size } });
}
