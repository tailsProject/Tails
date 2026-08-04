import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import styles from './AuthPage.module.scss';

// 소셜 로그인 성공/실패 모두 이 경로(oauth2.success-redirect-url)로 리다이렉트된다.
// 성공 시: accessToken 없이 refreshToken 쿠키만 심긴 리다이렉트 -> reissue로 accessToken을 받아온다.
// 신규 가입이면 ?new=true가 붙어서 가입완료(닉네임 확인) 화면으로 보낸다.
// 실패 시: ?error={코드}가 붙어서 온다.
export default function OAuth2RedirectPage() {
  const [searchParams] = useSearchParams();
  const { completeSessionFromCookie } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState(searchParams.get('error'));

  useEffect(() => {
    if (error) {
      return;
    }
    const isNewMember = searchParams.get('new') === 'true';
    completeSessionFromCookie()
      .then(() => navigate(isNewMember ? '/complete-profile' : '/'))
      .catch(() => setError('login_failed'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (error) {
    return (
      <div className={styles.wrapper}>
        <h1>로그인 실패</h1>
        <p className={styles.hintError}>
          {error === 'recently_withdrawn' && '탈퇴 후 24시간 동안은 같은 이메일로 재가입할 수 없습니다.'}
          {error === 'join_failed' && '회원가입에 실패했습니다. 다시 시도해주세요.'}
          {error !== 'recently_withdrawn' &&
            error !== 'join_failed' &&
            '소셜 로그인 중 문제가 발생했습니다. 다시 시도해주세요.'}
        </p>
      </div>
    );
  }

  return (
    <div className={styles.wrapper}>
      <p>로그인 처리 중입니다...</p>
    </div>
  );
}
