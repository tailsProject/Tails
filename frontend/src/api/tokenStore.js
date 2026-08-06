// 액세스 토큰 메모리 저장소, 새로고침 시 초기화됨
let accessToken = null;

export function getAccessToken() {
  return accessToken;
}

export function setAccessToken(token) {
  accessToken = token;
}
