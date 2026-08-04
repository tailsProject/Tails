import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { updateMyInfo } from '../mypage/api';
import { TERMS_CONTENT } from './termsContent';
import Button from '../../components/Button/Button';
import Modal from '../../components/Modal/Modal';
import styles from './AuthPage.module.scss';

// 소셜 로그인 첫 가입 직후 보여주는 가입완료 화면. 닉네임은 소셜 프로필에서 자동으로 채워져 있어
// 그대로 시작해도 되고, 원하면 바로 바꿀 수 있다. 일반 가입과 동일하게 필수 약관 동의를 받는다.
export default function CompleteProfilePage() {
  const { member, refreshMember } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [nickname, setNickname] = useState(member?.nickname ?? '');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [agreePrivacy, setAgreePrivacy] = useState(false);
  const [agreeMarketing, setAgreeMarketing] = useState(false);
  const [openTerms, setOpenTerms] = useState(null); // null | 'terms' | 'privacy' | 'marketing'

  const requiredAgreed = agreeTerms && agreePrivacy;
  const allAgreed = requiredAgreed && agreeMarketing;

  function handleAgreeAll(checked) {
    setAgreeTerms(checked);
    setAgreePrivacy(checked);
    setAgreeMarketing(checked);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await updateMyInfo({
        nickname: nickname !== member?.nickname ? nickname : undefined,
        marketingAgreed: agreeMarketing,
      });
      await refreshMember();
      navigate('/');
    } catch (error) {
      const message = error.response?.data?.error?.message ?? '가입 완료 처리에 실패했습니다.';
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

        <div className={styles.terms}>
          <label className={styles.termsAll}>
            <input type="checkbox" checked={allAgreed} onChange={(e) => handleAgreeAll(e.target.checked)} />
            약관 전체 동의
          </label>
          <label className={styles.termsItem}>
            <input type="checkbox" checked={agreeTerms} onChange={(e) => setAgreeTerms(e.target.checked)} />
            <span>[필수] 이용약관 동의</span>
            <button type="button" className={styles.termsMore} onClick={() => setOpenTerms('terms')}>
              더보기
            </button>
          </label>
          <label className={styles.termsItem}>
            <input type="checkbox" checked={agreePrivacy} onChange={(e) => setAgreePrivacy(e.target.checked)} />
            <span>[필수] 개인정보 수집·이용 동의</span>
            <button type="button" className={styles.termsMore} onClick={() => setOpenTerms('privacy')}>
              더보기
            </button>
          </label>
          <label className={styles.termsItem}>
            <input type="checkbox" checked={agreeMarketing} onChange={(e) => setAgreeMarketing(e.target.checked)} />
            <span>[선택] 마케팅 정보 수신 동의</span>
            <button type="button" className={styles.termsMore} onClick={() => setOpenTerms('marketing')}>
              더보기
            </button>
          </label>
        </div>

        <Modal open={openTerms !== null} onClose={() => setOpenTerms(null)}>
          {openTerms && (
            <div className={styles.termsModal}>
              <h2>{TERMS_CONTENT[openTerms].title}</h2>
              <p className={styles.termsBody}>{TERMS_CONTENT[openTerms].body}</p>
              <Button type="button" onClick={() => setOpenTerms(null)}>
                닫기
              </Button>
            </div>
          )}
        </Modal>

        <Button type="submit" disabled={isSubmitting || !requiredAgreed}>
          시작하기
        </Button>
      </form>
    </div>
  );
}
