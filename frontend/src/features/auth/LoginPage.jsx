import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import Button from '../../components/Button/Button';
import { KakaoIcon, GoogleIcon, NaverIcon } from '../../components/Icon/Icon';
import styles from './AuthPage.module.scss';

const API_BASE = import.meta.env.VITE_API_BASE_URL;

const SOCIAL_PROVIDERS = [
  { id: 'kakao', label: '카카오로 로그인', Icon: KakaoIcon },
  { id: 'google', label: '구글로 로그인', Icon: GoogleIcon },
  { id: 'naver', label: '네이버로 로그인', Icon: NaverIcon },
];

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { login } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await login(email, password);
      navigate('/');
    } catch (error) {
      const message = error.response?.data?.error?.message ?? '로그인에 실패했습니다.';
      showToast(message, 'error');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className={styles.wrapper}>
      <h1>로그인</h1>
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
        <label>
          비밀번호
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        <Button type="submit" disabled={isSubmitting}>
          로그인
        </Button>
      </form>

      <div className={styles.socialDivider}>또는</div>
      <div className={styles.socialButtons}>
        {SOCIAL_PROVIDERS.map((provider) => (
          <a
            key={provider.id}
            href={`${API_BASE}/oauth2/authorization/${provider.id}`}
            className={`${styles.socialButton} ${styles[provider.id]}`}
          >
            <provider.Icon /> {provider.label}
          </a>
        ))}
      </div>
      <p className={styles.socialHint}>이미 이메일로 가입하셨다면 같은 이메일의 소셜 로그인이 자동으로 연동돼요.</p>

      <p className={styles.switchLink}>
        계정이 없으신가요? <Link to="/signup">회원가입</Link>
      </p>
      <p className={styles.switchLink}>
        <Link to="/forgot-password">비밀번호 찾기</Link>
      </p>
    </div>
  );
}
