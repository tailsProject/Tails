import client from '../../api/client';

export function join({ email, password, passwordConfirm, nickname }) {
  return client.post('/api/members/join', { email, password, passwordConfirm, nickname });
}

export function checkEmail(email) {
  return client.get('/api/members/check-email', { params: { email } });
}

export function checkNickname(nickname) {
  return client.get('/api/members/check-nickname', { params: { nickname } });
}

// 소셜 로그인 첫 가입 직후 가입완료(닉네임 확인) 화면에서만 사용 - 마이페이지 쪽 api가
// 아직 없어서(별도 이슈) 임시로 여기 둠. 마이페이지 구현 시 정리 필요.
export function updateMyInfo({ nickname }) {
  return client.patch('/api/members/me', { nickname });
}
