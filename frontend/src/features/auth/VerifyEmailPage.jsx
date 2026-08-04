// 이메일 인증 링크 클릭 시 진입하는 페이지
import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { verifyEmail } from './api';
import styles from './AuthPage.module.scss';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [status, setStatus] = useState('loading');
  const [errorMessage, setErrorMessage] = useState('');

  // StrictMode 이중 렌더링에도 인증 요청이 두 번 나가지 않도록 방지
  const requestedRef = useRef(false);

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setErrorMessage('유효하지 않은 인증 링크입니다.');
      return;
    }
    if (requestedRef.current) {
      return;
    }
    requestedRef.current = true;
    verifyEmail(token)
      .then(() => setStatus('success'))
      .catch((error) => {
        setStatus('error');
        setErrorMessage(error.response?.data?.error?.message ?? '이메일 인증에 실패했습니다.');
      });
  }, [token]);

  return (
    <div className={styles.wrapper}>
      <h1>이메일 인증</h1>
      {status === 'loading' && <p>인증 처리 중입니다...</p>}
      {status === 'success' && <p className={styles.hintOk}>이메일 인증이 완료됐습니다.</p>}
      {status === 'error' && <p className={styles.hintError}>{errorMessage}</p>}
      <p className={styles.switchLink}>
        <Link to="/login">로그인 페이지로 이동</Link>
      </p>
    </div>
  );
}
