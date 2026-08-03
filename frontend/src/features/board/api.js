// 게시글 관련 API 호출 모음
import client from '../../api/client';

export function getBoards({ page = 0, size = 12, keyword, sortBy } = {}) {
  return client.get('/api/boards', { params: { page, size, keyword, sortBy } });
}

export function getBoardDetail(boardId) {
  return client.get(`/api/boards/${boardId}`);
}

export function createBoard({ title, content }) {
  return client.post('/api/boards', { title, content });
}

export function updateBoard(boardId, { title, content }) {
  return client.patch(`/api/boards/${boardId}`, { title, content });
}

export function deleteBoard(boardId) {
  return client.delete(`/api/boards/${boardId}`);
}

export function toggleLike(boardId) {
  return client.post(`/api/boards/${boardId}/like`);
}

export function toggleBookmark(boardId) {
  return client.post(`/api/boards/${boardId}/bookmark`);
}

export function uploadImages(boardId, files) {
  const formData = new FormData();
  files.forEach((file) => formData.append('files', file));
  return client.post(`/api/boards/${boardId}/images`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export function getImages(boardId) {
  return client.get(`/api/boards/${boardId}/images`);
}

export function reorderImages(boardId, imageIds) {
  return client.patch(`/api/boards/${boardId}/images/order`, { imageIds });
}

export function deleteImage(imageId) {
  return client.delete(`/api/images/${imageId}`);
}

export function getComments(boardId, { page = 0, size = 20 } = {}) {
  return client.get(`/api/boards/${boardId}/comments`, { params: { page, size } });
}

export function createComment(boardId, { content, parentId = null }) {
  return client.post(`/api/boards/${boardId}/comments`, { content, parentId });
}

export function updateComment(boardId, commentId, content) {
  return client.patch(`/api/boards/${boardId}/comments/${commentId}`, { content });
}

export function deleteComment(boardId, commentId) {
  return client.delete(`/api/boards/${boardId}/comments/${commentId}`);
}
