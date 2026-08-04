import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { verifyEmail } from './api';
import styles from './AuthPage.module.scss';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [status, setStatus] = useState('verifying'); // 'verifying' | 'success' | 'error'
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setErrorMessage('유효하지 않은 링크입니다.');
      return;
    }
    verifyEmail(token)
      .then(() => setStatus('success'))
      .catch((error) => {
        setStatus('error');
        setErrorMessage(error.response?.data?.error?.message ?? '이메일 인증에 실패했습니다.');
      });
  }, [token]);

  if (status === 'verifying') {
    return (
      <div className={styles.wrapper}>
        <h1>이메일 인증</h1>
        <p>인증 처리 중입니다...</p>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div className={styles.wrapper}>
        <h1>이메일 인증</h1>
        <p className={styles.hintError}>{errorMessage}</p>
        <p className={styles.switchLink}>
          <Link to="/login">로그인으로 돌아가기</Link>
        </p>
      </div>
    );
  }

  return (
    <div className={styles.wrapper}>
      <h1>이메일 인증</h1>
      <p className={styles.hintOk}>이메일 인증이 완료됐습니다.</p>
      <p className={styles.switchLink}>
        <Link to="/login">로그인하러 가기</Link>
      </p>
    </div>
  );
}
