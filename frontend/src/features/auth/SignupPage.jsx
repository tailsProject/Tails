import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { join, checkEmail, checkNickname } from './api';
import { useToast } from '../../hooks/useToast';
import Button from '../../components/Button/Button';
import styles from './AuthPage.module.scss';

// 백엔드 MemberJoinRequest 검증 규칙과 동일 (프론트에서 미리 걸러 UX 개선용, 최종 검증은 서버가 함)
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD_PATTERN = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).+$/;

export default function SignupPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [nickname, setNickname] = useState('');
  const [emailAvailable, setEmailAvailable] = useState(null); // null: 미확인
  const [nicknameAvailable, setNicknameAvailable] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { showToast } = useToast();
  const navigate = useNavigate();

  // 입력을 멈추고 500ms 뒤에만 중복 확인 호출 (매 키 입력마다 API를 부르지 않도록)
  useEffect(() => {
    if (!EMAIL_PATTERN.test(email) || email.length > 100) {
      setEmailAvailable(null);
      return;
    }
    const timer = setTimeout(async () => {
      const res = await checkEmail(email);
      setEmailAvailable(res.data.data.available);
    }, 500);
    return () => clearTimeout(timer);
  }, [email]);

  useEffect(() => {
    if (nickname.length < 2 || nickname.length > 20) {
      setNicknameAvailable(null);
      return;
    }
    const timer = setTimeout(async () => {
      const res = await checkNickname(nickname);
      setNicknameAvailable(res.data.data.available);
    }, 500);
    return () => clearTimeout(timer);
  }, [nickname]);

  const passwordValid = password.length >= 8 && password.length <= 50 && PASSWORD_PATTERN.test(password);
  const passwordMatches = passwordConfirm.length > 0 && password === passwordConfirm;

  async function handleSubmit(e) {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await join({ email, password, passwordConfirm, nickname });
      showToast('회원가입이 완료됐습니다. 로그인해주세요.', 'success');
      navigate('/login');
    } catch (error) {
      const message = error.response?.data?.error?.message ?? '회원가입에 실패했습니다.';
      showToast(message, 'error');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className={styles.wrapper}>
      <h1>회원가입</h1>
      <form onSubmit={handleSubmit} className={styles.form}>
        <label>
          이메일
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            maxLength={100}
            autoComplete="email"
            required
          />
          {emailAvailable === true && <span className={styles.hintOk}>사용 가능한 이메일입니다.</span>}
          {emailAvailable === false && <span className={styles.hintError}>이미 사용 중인 이메일입니다.</span>}
        </label>

        <label>
          비밀번호
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="new-password"
            required
          />
          {password.length > 0 && !passwordValid && (
            <span className={styles.hintError}>8~50자, 영문·숫자·특수문자를 모두 포함해야 합니다.</span>
          )}
        </label>

        <label>
          비밀번호 확인
          <input
            type="password"
            value={passwordConfirm}
            onChange={(e) => setPasswordConfirm(e.target.value)}
            autoComplete="new-password"
            required
          />
          {passwordConfirm.length > 0 && !passwordMatches && (
            <span className={styles.hintError}>비밀번호가 일치하지 않습니다.</span>
          )}
        </label>

        <label>
          닉네임
          <input
            type="text"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            minLength={2}
            maxLength={20}
            required
          />
          {nicknameAvailable === true && <span className={styles.hintOk}>사용 가능한 닉네임입니다.</span>}
          {nicknameAvailable === false && <span className={styles.hintError}>이미 사용 중인 닉네임입니다.</span>}
        </label>

        <Button type="submit" disabled={isSubmitting}>
          가입하기
        </Button>
      </form>
      <p className={styles.switchLink}>
        이미 계정이 있으신가요? <Link to="/login">로그인</Link>
      </p>
    </div>
  );
}
