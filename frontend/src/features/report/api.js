// 신고 등록 API 호출
import client from '../../api/client';

export function createReport({ targetType, targetId, reason }) {
  return client.post('/api/reports', { targetType, targetId, reason });
}
