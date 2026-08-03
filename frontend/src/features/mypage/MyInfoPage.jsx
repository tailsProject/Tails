// 내 정보 조회, 닉네임/비밀번호/프로필사진 수정, 탈퇴 담당
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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
import { resolveImage } from '../../utils/resolveImage';
import {
  CameraIcon,
  PawIcon,
  PencilIcon,
  CheckIcon,
  XMarkIcon,
  SuitcaseIcon,
  BookmarkIcon,
  StarIcon,
} from '../../components/Icon/Icon';
import styles from './MyInfoPage.module.scss';

export default function MyInfoPage() {
  const { refreshMember, logout } = useAuth();
  const { showToast } = useToast();
  const confirm = useConfirm();
  const navigate = useNavigate();

  const [info, setInfo] = useState(null);
  const [stats, setStats] = useState(null);

  const [editingNickname, setEditingNickname] = useState(false);
  const [nicknameDraft, setNicknameDraft] = useState('');
  const [savingNickname, setSavingNickname] = useState(false);

  const [editingPassword, setEditingPassword] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [savingPassword, setSavingPassword] = useState(false);

  async function load() {
    const [infoRes, statsRes] = await Promise.all([getMyInfo(), getMyStats()]);
    setInfo(infoRes.data.data);
    setStats(statsRes.data.data);
  }

  useEffect(() => {
    load();
  }, []);

  function openNicknameEdit() {
    setNicknameDraft(info.nickname);
    setEditingNickname(true);
  }

  async function handleNicknameSubmit(e) {
    e.preventDefault();
    setSavingNickname(true);
    try {
      await updateMyInfo({ nickname: nicknameDraft });
      await refreshMember();
      showToast('닉네임이 변경되었습니다.', 'success');
      setEditingNickname(false);
      await load();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '변경에 실패했습니다.', 'error');
    } finally {
      setSavingNickname(false);
    }
  }

  async function handleMarketingToggle(checked) {
    try {
      await updateMyInfo({ marketingAgreed: checked });
      showToast(checked ? '마케팅 정보 수신에 동의했습니다.' : '마케팅 정보 수신 동의를 철회했습니다.', 'success');
      load();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '변경에 실패했습니다.', 'error');
    }
  }

  async function handleProfileImageChange(e) {
    const file = e.target.files[0];
    if (!file) return;
    try {
      await uploadProfileImage(file);
      await refreshMember();
      showToast('프로필 이미지가 변경되었습니다.', 'success');
      load();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '이미지 변경에 실패했습니다.', 'error');
    } finally {
      e.target.value = '';
    }
  }

  async function handleProfileImageDelete() {
    await deleteProfileImage();
    await refreshMember();
    load();
  }

  function closePasswordEdit() {
    setEditingPassword(false);
    setCurrentPassword('');
    setNewPassword('');
    setNewPasswordConfirm('');
  }

  async function handlePasswordSubmit(e) {
    e.preventDefault();
    setSavingPassword(true);
    try {
      await changePassword({ currentPassword, newPassword, newPasswordConfirm });
      showToast('비밀번호가 변경되었습니다.', 'success');
      closePasswordEdit();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '변경에 실패했습니다.', 'error');
    } finally {
      setSavingPassword(false);
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
      <section className={styles.profileCard}>
        <div className={styles.avatarWrap}>
          {info.profileImg ? (
            <img className={styles.avatar} src={resolveImage(info.profileImg)} alt="" />
          ) : (
            <div className={styles.avatar}>
              <span className={styles.avatarPlaceholder}>
                <PawIcon />
              </span>
            </div>
          )}
          <label className={styles.avatarEditBtn}>
            <CameraIcon />
            <input
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              onChange={handleProfileImageChange}
              hidden
            />
          </label>
        </div>

        <div className={styles.profileBody}>
          {editingNickname ? (
            <form onSubmit={handleNicknameSubmit} className={styles.nicknameForm}>
              <input
                className={styles.nicknameInput}
                value={nicknameDraft}
                onChange={(e) => setNicknameDraft(e.target.value)}
                minLength={2}
                maxLength={20}
                autoFocus
              />
              <button type="submit" className={styles.iconBtnConfirm} disabled={savingNickname} aria-label="저장">
                <CheckIcon />
              </button>
              <button
                type="button"
                className={styles.iconBtnCancel}
                onClick={() => setEditingNickname(false)}
                aria-label="취소"
              >
                <XMarkIcon />
              </button>
            </form>
          ) : (
            <div className={styles.nameRow}>
              <h1 className={styles.nickname}>{info.nickname}</h1>
              <button type="button" className={styles.editIconBtn} onClick={openNicknameEdit} aria-label="닉네임 수정">
                <PencilIcon />
              </button>
            </div>
          )}

          <p className={styles.email}>
            {info.email}
            {info.emailVerified ? (
              <span className={styles.badgeVerified}>인증됨</span>
            ) : (
              <span className={styles.badgeMuted}>미인증</span>
            )}
          </p>

          <div className={styles.metaRow}>
            <span className={styles.metaBadge}>{info.provider ? `소셜 로그인 (${info.provider})` : '이메일 가입'}</span>
            <span className={styles.metaBadge}>{new Date(info.createdAt).toLocaleDateString()} 가입</span>
            {info.profileImg && (
              <button type="button" className={styles.removeAvatarLink} onClick={handleProfileImageDelete}>
                사진 삭제
              </button>
            )}
          </div>
        </div>
      </section>

      <section className={styles.statsRow}>
        <Link to="/travels" className={styles.statTile}>
          <span className={styles.statIcon}>
            <SuitcaseIcon />
          </span>
          <div>
            <span className={styles.statValue}>{stats.travelCount}</span>
            <span className={styles.statLabel}>여행 일정</span>
          </div>
        </Link>
        <Link to="/mypage/bookmarks" className={styles.statTile}>
          <span className={styles.statIcon}>
            <BookmarkIcon />
          </span>
          <div>
            <span className={styles.statValue}>{stats.placeBookmarkCount}</span>
            <span className={styles.statLabel}>찜한 장소</span>
          </div>
        </Link>
        <Link to="/mypage/reviews" className={styles.statTile}>
          <span className={styles.statIcon}>
            <StarIcon />
          </span>
          <div>
            <span className={styles.statValue}>{stats.reviewCount}</span>
            <span className={styles.statLabel}>작성 리뷰</span>
          </div>
        </Link>
      </section>

      <section className={styles.settingsCard}>
        <h2>계정 설정</h2>

        <div className={styles.settingRow}>
          <div className={styles.settingText}>
            <span className={styles.settingLabel}>비밀번호</span>
            <span className={styles.settingDesc}>
              {info.provider
                ? '소셜 로그인 계정은 별도 비밀번호 변경이 필요 없습니다.'
                : '주기적으로 변경하면 계정을 더 안전하게 지킬 수 있어요.'}
            </span>
          </div>
          {!info.provider && (
            <button
              type="button"
              className={styles.editIconBtn}
              onClick={() => (editingPassword ? closePasswordEdit() : setEditingPassword(true))}
              aria-label={editingPassword ? '비밀번호 변경 닫기' : '비밀번호 변경'}
            >
              {editingPassword ? <XMarkIcon /> : <PencilIcon />}
            </button>
          )}
        </div>

        {editingPassword && (
          <form onSubmit={handlePasswordSubmit} className={styles.passwordForm}>
            <label className={styles.passwordField}>
              <span className={styles.fieldLabel}>현재 비밀번호</span>
              <input
                type="password"
                placeholder="현재 비밀번호를 입력하세요"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
              />
            </label>
            <label className={styles.passwordField}>
              <span className={styles.fieldLabel}>새 비밀번호</span>
              <input
                type="password"
                placeholder="새 비밀번호를 입력하세요"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />
            </label>
            <label className={styles.passwordField}>
              <span className={styles.fieldLabel}>새 비밀번호 확인</span>
              <input
                type="password"
                placeholder="새 비밀번호를 한 번 더 입력하세요"
                value={newPasswordConfirm}
                onChange={(e) => setNewPasswordConfirm(e.target.value)}
                required
              />
            </label>
            <div className={styles.passwordFormActions}>
              <button type="submit" className={styles.smallBtn} disabled={savingPassword}>
                <CheckIcon /> 저장
              </button>
            </div>
          </form>
        )}

        <div className={styles.settingRow}>
          <div className={styles.settingText}>
            <span className={styles.settingLabel}>마케팅 정보 수신</span>
            <span className={styles.settingDesc}>이벤트/혜택 등 마케팅 정보를 이메일로 받아봅니다.</span>
          </div>
          <label className={styles.switch}>
            <input
              type="checkbox"
              checked={info.marketingAgreed}
              onChange={(e) => handleMarketingToggle(e.target.checked)}
            />
            <span className={styles.slider} />
          </label>
        </div>

        <div className={styles.settingRow}>
          <div className={styles.settingText}>
            <span className={styles.settingLabel}>회원 탈퇴</span>
            <span className={styles.settingDesc}>탈퇴 시 계정은 복구할 수 없습니다.</span>
          </div>
          <button type="button" className={styles.withdrawLink} onClick={handleWithdraw}>
            탈퇴
          </button>
        </div>
      </section>
    </div>
  );
}
