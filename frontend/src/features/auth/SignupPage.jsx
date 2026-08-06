// 회원가입 페이지, 이메일 인증과 약관 동의를 포함
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { join, checkEmail, checkNickname, sendSignupCode, verifySignupCode } from './api';
import { TERMS_CONTENT } from './termsContent';
import { useToast } from '../../hooks/useToast';
import Button from '../../components/Button/Button';
import Modal from '../../components/Modal/Modal';
import styles from './AuthPage.module.scss';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD_PATTERN = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).+$/;
// 공백과 자음/모음 단독 문자 차단, 특수문자와 이모지는 허용
const NICKNAME_PATTERN = /^[^\sㄱ-ㆎ]+$/;

export default function SignupPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [nickname, setNickname] = useState('');
  const [emailAvailable, setEmailAvailable] = useState(null);
  const [emailUnavailableReason, setEmailUnavailableReason] = useState('');
  const [nicknameAvailable, setNicknameAvailable] = useState(null);
  const [code, setCode] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [codeVerified, setCodeVerified] = useState(false);
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [isVerifyingCode, setIsVerifyingCode] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [agreePrivacy, setAgreePrivacy] = useState(false);
  const [agreeMarketing, setAgreeMarketing] = useState(false);
  const [openTerms, setOpenTerms] = useState(null); 
  const { showToast } = useToast();
  const navigate = useNavigate();

  const requiredAgreed = agreeTerms && agreePrivacy;
  const allAgreed = requiredAgreed && agreeMarketing;

  function handleAgreeAll(checked) {
    setAgreeTerms(checked);
    setAgreePrivacy(checked);
    setAgreeMarketing(checked);
  }

  // 이메일 형식이 맞을 때만 0.5초 디바운스로 중복확인 요청
  useEffect(() => {
    if (!EMAIL_PATTERN.test(email) || email.length > 100) {
      setEmailAvailable(null);
      return;
    }
    const timer = setTimeout(async () => {
      const res = await checkEmail(email);
      setEmailAvailable(res.data.data.available);
      setEmailUnavailableReason(res.data.data.reason ?? '');
    }, 500);
    return () => clearTimeout(timer);
  }, [email]);

  useEffect(() => {
    setCode('');
    setCodeSent(false);
    setCodeVerified(false);
  }, [email]);

  useEffect(() => {
    if (nickname.length < 2 || nickname.length > 20 || !NICKNAME_PATTERN.test(nickname)) {
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
  const nicknameFormatValid = nickname.length === 0 || NICKNAME_PATTERN.test(nickname);

  async function handleSendCode() {
    setIsSendingCode(true);
    try {
      await sendSignupCode(email);
      setCodeSent(true);
      showToast('인증번호를 발송했습니다. (5분간 유효)', 'success');
    } catch (error) {
      const message = error.response?.data?.error?.message ?? '인증번호 발송에 실패했습니다.';
      showToast(message, 'error');
    } finally {
      setIsSendingCode(false);
    }
  }

  async function handleVerifyCode() {
    setIsVerifyingCode(true);
    try {
      await verifySignupCode({ email, code });
      setCodeVerified(true);
      showToast('이메일 인증이 완료됐습니다.', 'success');
    } catch (error) {
      const message = error.response?.data?.error?.message ?? '인증번호가 올바르지 않습니다.';
      showToast(message, 'error');
    } finally {
      setIsVerifyingCode(false);
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await join({ email, password, passwordConfirm, nickname, agreeMarketing });
      showToast('회원가입이 완료됐습니다.', 'success');
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
          <div className={styles.inlineField}>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              maxLength={100}
              autoComplete="email"
              disabled={codeVerified}
              required
            />
            <Button
              type="button"
              variant="secondary"
              onClick={handleSendCode}
              disabled={!EMAIL_PATTERN.test(email) || emailAvailable !== true || isSendingCode || codeVerified}
            >
              {codeSent ? '재전송' : '인증'}
            </Button>
          </div>
          {emailAvailable === true && <span className={styles.hintOk}>사용 가능한 이메일입니다.</span>}
          {emailAvailable === false && <span className={styles.hintError}>{emailUnavailableReason}</span>}
        </label>

        {codeSent && !codeVerified && (
          <label>
            인증번호
            <div className={styles.inlineField}>
              <input
                type="text"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                maxLength={6}
                placeholder="6자리 숫자"
                required
              />
              <Button type="button" variant="secondary" onClick={handleVerifyCode} disabled={isVerifyingCode}>
                확인
              </Button>
            </div>
          </label>
        )}
        {codeVerified && <span className={styles.hintOk}>이메일 인증 완료</span>}

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
          {!nicknameFormatValid && (
            <span className={styles.hintError}>닉네임에 사용할 수 없는 문자가 포함되어 있습니다.</span>
          )}
          {nicknameFormatValid && nicknameAvailable === true && (
            <span className={styles.hintOk}>사용 가능한 닉네임입니다.</span>
          )}
          {nicknameFormatValid && nicknameAvailable === false && (
            <span className={styles.hintError}>이미 사용 중인 닉네임입니다.</span>
          )}
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

        <Button type="submit" disabled={isSubmitting || !codeVerified || !requiredAgreed}>
          가입하기
        </Button>
      </form>
      <p className={styles.switchLink}>
        이미 계정이 있으신가요? <Link to="/login">로그인</Link>
      </p>
    </div>
  );
}
