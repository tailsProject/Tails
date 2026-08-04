// 관리자 회원 관리 API 호출 모음
import client from '../../api/client';

export function getMembers({ keyword, page = 0, size = 20 } = {}) {
  return client.get('/api/admin/members', { params: { keyword: keyword || undefined, page, size } });
}

export function changeMemberRole(memberId, role) {
  return client.patch(`/api/admin/members/${memberId}/role`, { role });
}

export function expelMember(memberId) {
  return client.delete(`/api/admin/members/${memberId}`);
}
