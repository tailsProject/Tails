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

export function getImages(boardId) {
  return client.get(`/api/boards/${boardId}/images`);
}
