// 백엔드 에러코드 패턴 판별용 유틸
export function isNotFoundError(code) {
  return typeof code === 'string' && code.endsWith('_NOT_FOUND');
}

export function isOwnershipError(code) {
  return typeof code === 'string' && code.startsWith('NOT_') && code.endsWith('_OWNER');
}

export const CONCURRENT_UPDATE_CONFLICT = 'CONCURRENT_UPDATE_CONFLICT';

export const ACCOUNT_LOCKED = 'ACCOUNT_LOCKED';
