import client from '../../api/client';

export function join({ email, password, passwordConfirm, nickname, agreeMarketing }) {
  return client.post('/api/members/join', { email, password, passwordConfirm, nickname, agreeMarketing });
}

export function checkEmail(email) {
  return client.get('/api/members/check-email', { params: { email } });
}

export function checkNickname(nickname) {
  return client.get('/api/members/check-nickname', { params: { nickname } });
}

export function sendSignupCode(email) {
  return client.post('/api/auth/email/signup-code', { email });
}

export function verifySignupCode({ email, code }) {
  return client.post('/api/auth/email/signup-code/verify', { email, code });
}

export function requestPasswordReset(email) {
  return client.post('/api/auth/password/reset-request', { email });
}

export function resetPassword({ token, newPassword, newPasswordConfirm }) {
  return client.post('/api/auth/password/reset', { token, newPassword, newPasswordConfirm });
}

export function requestEmailVerification(email) {
  return client.post('/api/auth/email/verify-request', { email });
}

export function verifyEmail(token) {
  return client.get('/api/auth/email/verify', { params: { token } });
}
