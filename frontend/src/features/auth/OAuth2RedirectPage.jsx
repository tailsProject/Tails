// 소셜 로그인 콜백 처리 페이지, 신규가입/재로그인 분기
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { WarningIcon } from '../../components/Icon/Icon';
import StateMessage from '../../components/StateMessage/StateMessage';
import styles from './AuthPage.module.scss';

export default function OAuth2RedirectPage() {
  const [searchParams] = useSearchParams();
  const { completeSessionFromCookie } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState(searchParams.get('error'));

  // 신규 소셜 가입이면 프로필 완성 페이지로, 기존 회원이면 메인으로 이동
  useEffect(() => {
    if (error) {
      return;
    }
    completeSessionFromCookie()
      .then(() => navigate(searchParams.get('new') === 'true' ? '/complete-profile' : '/'))
      .catch(() => setError('login_failed'));
  }, []);

  if (error) {
    const isRecentlyWithdrawn = error === 'recently_withdrawn';
    const description = isRecentlyWithdrawn
      ? '탈퇴하신 계정은 탈퇴 시점으로부터 24시간이 지난 후에 같은 이메일로 다시 가입하실 수 있습니다.'
      : error === 'join_failed'
        ? '회원가입에 실패했습니다. 다시 시도해주세요.'
        : '소셜 로그인 중 문제가 발생했습니다. 다시 시도해주세요.';
    return (
      <StateMessage
        icon={WarningIcon}
        title={isRecentlyWithdrawn ? '재가입 제한 안내' : '로그인 실패'}
        description={description}
        actionTo="/login"
        actionLabel="로그인 페이지로 이동"
      />
    );
  }

  return (
    <div className={styles.wrapper}>
      <p>로그인 처리 중입니다...</p>
    </div>
  );
}
