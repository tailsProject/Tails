// 비밀번호 재설정 링크로 진입해 새 비밀번호를 입력하는 페이지
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { resetPassword } from './api';
import { useToast } from '../../hooks/useToast';
import Button from '../../components/Button/Button';
import styles from './AuthPage.module.scss';

const PASSWORD_PATTERN = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).+$/;

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const passwordValid =
    newPassword.length >= 8 && newPassword.length <= 50 && PASSWORD_PATTERN.test(newPassword);
  const passwordMatches = newPasswordConfirm.length > 0 && newPassword === newPasswordConfirm;

  if (!token) {
    return (
      <div className={styles.wrapper}>
        <h1>비밀번호 재설정</h1>
        <p className={styles.hintError}>유효하지 않은 링크입니다.</p>
        <p className={styles.switchLink}>
          <Link to="/forgot-password">비밀번호 찾기 다시 요청하기</Link>
        </p>
      </div>
    );
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await resetPassword({ token, newPassword, newPasswordConfirm });
      showToast('비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.', 'success');
      navigate('/login');
    } catch (error) {
      const message = error.response?.data?.error?.message ?? '비밀번호 재설정에 실패했습니다.';
      showToast(message, 'error');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className={styles.wrapper}>
      <h1>비밀번호 재설정</h1>
      <form onSubmit={handleSubmit} className={styles.form}>
        <label>
          새 비밀번호
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            autoComplete="new-password"
            required
          />
          {newPassword.length > 0 && !passwordValid && (
            <span className={styles.hintError}>8~50자, 영문·숫자·특수문자를 모두 포함해야 합니다.</span>
          )}
        </label>
        <label>
          새 비밀번호 확인
          <input
            type="password"
            value={newPasswordConfirm}
            onChange={(e) => setNewPasswordConfirm(e.target.value)}
            autoComplete="new-password"
            required
          />
          {newPasswordConfirm.length > 0 && !passwordMatches && (
            <span className={styles.hintError}>비밀번호가 일치하지 않습니다.</span>
          )}
        </label>
        <Button type="submit" disabled={isSubmitting || !passwordValid || !passwordMatches}>
          비밀번호 변경
        </Button>
      </form>
    </div>
  );
}
