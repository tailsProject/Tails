// 비밀번호 재설정 링크 요청 페이지
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { requestPasswordReset } from './api';
import { useToast } from '../../hooks/useToast';
import Button from '../../components/Button/Button';
import styles from './AuthPage.module.scss';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [sent, setSent] = useState(false);
  const { showToast } = useToast();

  async function handleSubmit(e) {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await requestPasswordReset(email);
      setSent(true);
    } catch (error) {
      const message = error.response?.data?.error?.message ?? '요청에 실패했습니다.';
      showToast(message, 'error');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className={styles.wrapper}>
      <h1>비밀번호 찾기</h1>
      {sent ? (
        <p className={styles.hintOk}>
          입력하신 이메일로 비밀번호 재설정 링크를 보냈습니다. (가입된 이메일인 경우, 30분간 유효)
        </p>
      ) : (
        <>
          <p>가입하신 이메일 주소를 입력하면 비밀번호 재설정 링크를 보내드려요.</p>
          <form onSubmit={handleSubmit} className={styles.form}>
            <label>
              이메일
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                required
              />
            </label>
            <Button type="submit" disabled={isSubmitting}>
              재설정 링크 보내기
            </Button>
          </form>
        </>
      )}
      <p className={styles.switchLink}>
        <Link to="/login">로그인 페이지로 이동</Link>
      </p>
    </div>
  );
}
