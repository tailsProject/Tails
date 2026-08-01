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
