// 전역 헤더. 로그인 상태, 알림 드롭다운, 모바일 메뉴 담당
import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useNotifications } from '../../hooks/useNotifications';
import { markNotificationAsRead, markAllNotificationsAsRead, deleteAllNotifications } from '../../features/mypage/api';
import { useConfirm } from '../../hooks/useConfirm';
import { resolveProfileImage } from '../../utils/resolveImage';
import { BellIcon } from '../Icon/Icon';
import styles from './Header.module.scss';

const NAV_LINKS = [
  { to: '/', label: '메인', end: true },
  { to: '/places', label: '지도' },
  { to: '/boards', label: '피드' },
  { to: '/travels', label: '여행일정' },
  { to: '/mypage', label: '마이페이지' },
];

// 알림 종류별로 클릭 시 이동할 경로
const TYPE_TARGET_PATH = {
  COMMENT: (targetId) => `/boards/${targetId}`,
  REPLY: (targetId) => `/boards/${targetId}`,
  LIKE: (targetId) => `/boards/${targetId}`,
  BOOKMARK: (targetId) => `/boards/${targetId}`,
  TRAVEL: (targetId) => `/travels/${targetId}`,
};

export default function Header() {
  const { isAuthenticated, member } = useAuth();
  const { notifications, unreadCount, refresh: refreshNotifications } = useNotifications();
  const [notifOpen, setNotifOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const notifRef = useRef(null);
  const navigate = useNavigate();
  const confirm = useConfirm();

  useEffect(() => {
    function handleOutsideClick(event) {
      if (notifRef.current && !notifRef.current.contains(event.target)) {
        setNotifOpen(false);
      }
    }
    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, []);

  async function handleNotificationClick(notification) {
    if (!notification.read) {
      await markNotificationAsRead(notification.notificationId);
      refreshNotifications();
    }
    setNotifOpen(false);
    const pathFn = TYPE_TARGET_PATH[notification.type];
    navigate(pathFn ? pathFn(notification.targetId) : '/mypage/notifications');
  }

  async function handleReadAll() {
    await markAllNotificationsAsRead();
    refreshNotifications();
  }

  async function handleDeleteAll() {
    const ok = await confirm('알림을 전부 삭제하시겠습니까?');
    if (!ok) return;
    await deleteAllNotifications();
    refreshNotifications();
  }

  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <Link to="/" className={styles.logo}>
          <img src="/TailsLogo.png" alt="Tails" className={styles.logoImage} />
        </Link>
        <nav className={styles.nav}>
          {NAV_LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              className={({ isActive }) => (isActive ? styles.navActive : styles.navLink)}
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
        <div className={styles.auth}>
          {isAuthenticated ? (
            <>
              <Link to="/mypage" className={styles.profileLink}>
                <span className={styles.avatar}>
                  <img src={resolveProfileImage(member.profileImg)} alt="" />
                </span>
                <span>{member.nickname}님</span>
              </Link>
              <div className={styles.notifWrapper} ref={notifRef}>
                <button
                  className={styles.notificationLink}
                  aria-label="알림"
                  onClick={() => setNotifOpen((open) => !open)}
                >
                  <span className={styles.bellIcon}><BellIcon /></span>
                  {unreadCount > 0 && <span className={styles.badge}>{unreadCount > 99 ? '99+' : unreadCount}</span>}
                </button>
                {notifOpen && (
                  <div className={styles.notifPanel}>
                    <div className={styles.notifPanelHeader}>
                      <span>알림</span>
                      <div>
                        <button onClick={handleReadAll}>모두 읽음</button>
                        <button onClick={handleDeleteAll}>모두 삭제</button>
                      </div>
                    </div>
                    {notifications.length === 0 && <p className={styles.notifEmpty}>알림이 없습니다.</p>}
                    {notifications.map((n) => (
                      <button
                        key={n.notificationId}
                        className={`${styles.notifItem} ${!n.read ? styles.notifItemUnread : ''}`}
                        onClick={() => handleNotificationClick(n)}
                      >
                        <p>{n.content}</p>
                        <span>{new Date(n.createdAt).toLocaleString()}</span>
                      </button>
                    ))}
                    <Link to="/mypage/notifications" className={styles.notifPanelFooter} onClick={() => setNotifOpen(false)}>
                      전체 알림 보기
                    </Link>
                  </div>
                )}
              </div>
            </>
          ) : (
            <Link to="/login" className={styles.loginLink}>
              로그인
            </Link>
          )}
          <button
            className={styles.menuToggle}
            aria-label="메뉴"
            onClick={() => setMenuOpen((open) => !open)}
          >
            {menuOpen ? '✕' : '☰'}
          </button>
        </div>
      </div>

      {menuOpen && (
        <nav className={styles.mobileNav}>
          {NAV_LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              onClick={() => setMenuOpen(false)}
              className={({ isActive }) => (isActive ? styles.mobileNavActive : styles.mobileNavLink)}
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
      )}
    </header>
  );
}
