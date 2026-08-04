import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { updateMyInfo } from '../mypage/api';
import Button from '../../components/Button/Button';
import styles from './AuthPage.module.scss';

// 소셜 로그인 첫 가입 직후 보여주는 가입완료 화면. 닉네임은 소셜 프로필에서 자동으로 채워져 있어
// 그대로 시작해도 되고, 원하면 바로 바꿀 수 있다.
export default function CompleteProfilePage() {
  const { member, refreshMember } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [nickname, setNickname] = useState(member?.nickname ?? '');
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      if (nickname !== member?.nickname) {
        await updateMyInfo({ nickname });
        await refreshMember();
      }
      navigate('/');
    } catch (error) {
      const message = error.response?.data?.error?.message ?? '닉네임 변경에 실패했습니다.';
      showToast(message, 'error');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className={styles.wrapper}>
      <h1>가입을 환영합니다</h1>
      <p>닉네임을 확인하고 시작해주세요. 나중에 마이페이지에서 다시 바꿀 수 있어요.</p>
      <form onSubmit={handleSubmit} className={styles.form}>
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
        </label>
        <Button type="submit" disabled={isSubmitting}>
          시작하기
        </Button>
      </form>
    </div>
  );
}
