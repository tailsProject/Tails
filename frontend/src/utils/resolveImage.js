// 상대 경로 이미지 URL을 서버 주소와 합쳐 완전한 URL로 변환
const API_BASE = import.meta.env.VITE_API_BASE_URL;

export function resolveImage(url) {
  if (!url) return null;
  return url.startsWith('http') ? url : `${API_BASE}${url}`;
}
