// 관리자 사이드바 탭과 하위 라우트 뼈대
import { NavLink, Outlet } from 'react-router-dom';
import { WarningIcon, UserIcon, CrownIcon } from '../../components/Icon/Icon';
import styles from './AdminLayout.module.scss';

const TABS = [
  { to: '/admin/reports', label: '신고 처리', icon: WarningIcon },
  { to: '/admin/members', label: '회원 권한 변경', icon: UserIcon },
];

export default function AdminLayout() {
  return (
    <div className={styles.wrapper}>
      <aside className={styles.sidebar}>
        <div className={styles.sidebarHeader}>
          <span className={styles.sidebarIcon}>
            <CrownIcon />
          </span>
          <span>관리자</span>
        </div>

        <nav className={styles.tabs}>
          {TABS.map((tab) => (
            <NavLink key={tab.to} to={tab.to} className={({ isActive }) => (isActive ? styles.active : undefined)}>
              <span className={styles.tabIcon}>
                <tab.icon />
              </span>
              {tab.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className={styles.content}>
        <Outlet />
      </div>
    </div>
  );
}
