// 게시글/댓글 조회 API. Board 화면(박영준 트랙)이 아직 없어서 신고 버튼을 붙일
// 최소 뼈대 화면(BoardDetailPage)용으로 임시로 여기 둠. Board 프론트 구현 시 정리 필요.
import client from '../../api/client';

export function getBoardDetail(boardId) {
  return client.get(`/api/boards/${boardId}`);
}

export function getComments(boardId, { page = 0, size = 10 } = {}) {
  return client.get(`/api/boards/${boardId}/comments`, { params: { page, size } });
}
