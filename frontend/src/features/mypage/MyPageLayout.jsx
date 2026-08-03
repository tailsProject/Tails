import { NavLink, Outlet } from 'react-router-dom';
import styles from './MyPageLayout.module.scss';

const TABS = [
  { to: '/mypage', label: '내 정보', end: true },
  { to: '/mypage/pets', label: '반려동물' },
];

export default function MyPageLayout() {
  return (
    <div className={styles.wrapper}>
      <nav className={styles.tabs}>
        {TABS.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            end={tab.end}
            className={({ isActive }) => (isActive ? styles.active : undefined)}
          >
            {tab.label}
          </NavLink>
        ))}
      </nav>
      <div className={styles.content}>
        <Outlet />
      </div>
    </div>
  );
}
