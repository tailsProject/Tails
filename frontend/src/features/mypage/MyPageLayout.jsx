// 마이페이지 사이드바 탭과 하위 라우트 뼈대
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { isStaffRole } from '../../utils/memberRole';
import { UserIcon, PawIcon, PencilIcon, StarIcon, BookmarkIcon, WarningIcon, BellIcon, CrownIcon, LogoutIcon } from '../../components/Icon/Icon';
import styles from './MyPageLayout.module.scss';

const TABS = [
  { to: '/mypage', label: '내 정보', icon: UserIcon, end: true },
  { to: '/mypage/pets', label: '반려동물', icon: PawIcon },
  { to: '/mypage/boards', label: '내가 쓴 글', icon: PencilIcon },
  { to: '/mypage/reviews', label: '내가 쓴 리뷰', icon: StarIcon },
  { to: '/mypage/bookmarks', label: '북마크한 글', icon: BookmarkIcon },
  { to: '/mypage/reports', label: '내 신고 내역', icon: WarningIcon },
  { to: '/mypage/notifications', label: '알림', icon: BellIcon },
];

export default function MyPageLayout() {
  const { member, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate('/');
  }

  return (
    <div className={styles.wrapper}>
      <aside className={styles.sidebar}>
        <nav className={styles.tabs}>
          {TABS.map((tab) => (
            <NavLink
              key={tab.to}
              to={tab.to}
              end={tab.end}
              className={({ isActive }) => (isActive ? styles.active : undefined)}
            >
              <span className={styles.tabIcon}><tab.icon /></span>
              {tab.label}
            </NavLink>
          ))}
          {isStaffRole(member?.role) && (
            <NavLink to="/admin" className={({ isActive }) => (isActive ? styles.active : undefined)}>
              <span className={styles.tabIcon}><CrownIcon /></span>
              관리자
            </NavLink>
          )}
        </nav>

        <button type="button" className={styles.logoutBtn} onClick={handleLogout}>
          <LogoutIcon /> 로그아웃
        </button>
      </aside>
      <div className={styles.content}>
        <Outlet />
      </div>
    </div>
  );
}
