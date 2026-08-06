// 로그인 회원 정보와 인증 관련 동작 전역 제공
import { useEffect, useState } from 'react';
import client from '../api/client';
import { setAccessToken } from '../api/tokenStore';
import { AuthContext } from '../hooks/useAuth';

export function AuthProvider({ children }) {
  const [member, setMember] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  // 리프레시 쿠키로 액세스 토큰 재발급 후 내 정보 조회, 앱 첫 진입 시 로그인 세션 복구용
  async function completeSessionFromCookie() {
    const reissueRes = await client.post('/api/auth/reissue');
    setAccessToken(reissueRes.data.data.accessToken);
    const meRes = await client.get('/api/members/me');
    setMember({
      memberId: meRes.data.data.memberId,
      nickname: meRes.data.data.nickname,
      role: meRes.data.data.role,
      profileImg: meRes.data.data.profileImg,
    });
  }


  useEffect(() => {
    completeSessionFromCookie()
      .catch(() => {

      })
      .finally(() => setIsLoading(false));
  }, []);

  async function login(email, password) {
    const res = await client.post('/api/members/login', { email, password });
    const { accessToken, memberId, nickname, role, profileImg } = res.data.data;
    setAccessToken(accessToken);
    setMember({ memberId, nickname, role, profileImg });
  }

  async function logout() {
    try {
      await client.post('/api/auth/logout');
    } finally {
      setAccessToken(null);
      setMember(null);
    }
  }

  async function refreshMember() {
    const res = await client.get('/api/members/me');
    setMember({
      memberId: res.data.data.memberId,
      nickname: res.data.data.nickname,
      role: res.data.data.role,
      profileImg: res.data.data.profileImg,
    });
  }

  const value = {
    member,
    isAuthenticated: member !== null,
    isLoading,
    login,
    logout,
    refreshMember,
    completeSessionFromCookie,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
