import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getMyInfo,
  getMyStats,
  updateMyInfo,
  changePassword,
  uploadProfileImage,
  deleteProfileImage,
  withdraw,
} from './api';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { useConfirm } from '../../hooks/useConfirm';
import Button from '../../components/Button/Button';
import { resolveImage } from '../../utils/resolveImage';
import styles from './MyInfoPage.module.scss';

export default function MyInfoPage() {
  const { refreshMember, logout } = useAuth();
  const { showToast } = useToast();
  const confirm = useConfirm();
  const navigate = useNavigate();

  const [info, setInfo] = useState(null);
  const [stats, setStats] = useState(null);
  const [nickname, setNickname] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');

  async function load() {
    const [infoRes, statsRes] = await Promise.all([getMyInfo(), getMyStats()]);
    setInfo(infoRes.data.data);
    setNickname(infoRes.data.data.nickname);
    setStats(statsRes.data.data);
  }

  useEffect(() => {
    load();
  }, []);

  async function handleNicknameSubmit(e) {
    e.preventDefault();
    try {
      await updateMyInfo({ nickname });
      await refreshMember();
      showToast('닉네임이 변경되었습니다.', 'success');
      load();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '변경에 실패했습니다.', 'error');
    }
  }

  async function handleProfileImageChange(e) {
    const file = e.target.files[0];
    if (!file) return;
    await uploadProfileImage(file);
    await refreshMember();
    e.target.value = '';
    load();
  }

  async function handleProfileImageDelete() {
    await deleteProfileImage();
    await refreshMember();
    load();
  }

  async function handlePasswordSubmit(e) {
    e.preventDefault();
    try {
      await changePassword({ currentPassword, newPassword, newPasswordConfirm });
      showToast('비밀번호가 변경되었습니다.', 'success');
      setCurrentPassword('');
      setNewPassword('');
      setNewPasswordConfirm('');
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '변경에 실패했습니다.', 'error');
    }
  }

  async function handleWithdraw() {
    const ok = await confirm(
      '정말 탈퇴하시겠습니까?\n작성한 글/댓글/리뷰는 "탈퇴한 회원"으로 남고, 되돌릴 수 없습니다.',
    );
    if (!ok) return;
    await withdraw();
    await logout();
    navigate('/');
  }

  if (!info || !stats) {
    return null;
  }

  return (
    <div className={styles.wrapper}>
      <section className={styles.profile}>
        <div className={styles.avatar}>
          {info.profileImg ? (
            <img src={resolveImage(info.profileImg)} alt="" />
          ) : (
            <div className={styles.avatarPlaceholder}>{info.nickname[0]}</div>
          )}
        </div>
        <div className={styles.avatarActions}>
          <label className={styles.uploadLabel}>
            이미지 변경
            <input
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              onChange={handleProfileImageChange}
              hidden
            />
          </label>
          {info.profileImg && <button onClick={handleProfileImageDelete}>삭제</button>}
        </div>
        <dl className={styles.infoList}>
          <dt>이메일</dt>
          <dd>
            {info.email}{' '}
            {info.emailVerified ? (
              <span className={styles.verified}>인증됨</span>
            ) : (
              <span className={styles.unverified}>미인증</span>
            )}
          </dd>
          <dt>가입 방식</dt>
          <dd>{info.provider ? `소셜 로그인(${info.provider})` : '이메일 가입'}</dd>
          <dt>가입일</dt>
          <dd>{new Date(info.createdAt).toLocaleDateString()}</dd>
        </dl>
      </section>

      <section className={styles.stats}>
        <div className={styles.statItem}>
          <span className={styles.statValue}>{stats.travelCount}</span>
          <span className={styles.statLabel}>여행 일정</span>
        </div>
        <div className={styles.statItem}>
          <span className={styles.statValue}>{stats.placeBookmarkCount}</span>
          <span className={styles.statLabel}>찜한 장소</span>
        </div>
        <div className={styles.statItem}>
          <span className={styles.statValue}>{stats.reviewCount}</span>
          <span className={styles.statLabel}>작성 리뷰</span>
        </div>
      </section>

      <section className={styles.section}>
        <h2>닉네임 변경</h2>
        <form onSubmit={handleNicknameSubmit} className={styles.inlineForm}>
          <input value={nickname} onChange={(e) => setNickname(e.target.value)} minLength={2} maxLength={20} />
          <Button type="submit">저장</Button>
        </form>
      </section>

      <section className={styles.section}>
        <h2>비밀번호 변경</h2>
        {info.provider ? (
          <p className={styles.hint}>소셜 로그인 계정은 별도 비밀번호 변경이 필요 없습니다.</p>
        ) : (
          <form onSubmit={handlePasswordSubmit} className={styles.form}>
            <input
              type="password"
              placeholder="현재 비밀번호"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              required
            />
            <input
              type="password"
              placeholder="새 비밀번호"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
            />
            <input
              type="password"
              placeholder="새 비밀번호 확인"
              value={newPasswordConfirm}
              onChange={(e) => setNewPasswordConfirm(e.target.value)}
              required
            />
            <Button type="submit">비밀번호 변경</Button>
          </form>
        )}
      </section>

      <section className={styles.dangerZone}>
        <h2>회원 탈퇴</h2>
        <p className={styles.hint}>탈퇴 시 계정은 복구할 수 없습니다.</p>
        <Button variant="secondary" onClick={handleWithdraw}>
          회원 탈퇴
        </Button>
      </section>
    </div>
  );
}
