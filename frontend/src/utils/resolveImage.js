// 상대 경로 이미지 URL을 서버 주소와 합쳐 완전한 URL로 변환
const API_BASE = import.meta.env.VITE_API_BASE_URL;

export function resolveImage(url) {
  if (!url) return null;
  return url.startsWith('http') ? url : `${API_BASE}${url}`;
}

// 프로필 사진 미설정 시 기본 프로필 이미지로 대체
export function resolveProfileImage(url) {
  return resolveImage(url) ?? '/DefaultProfile.png';
}
