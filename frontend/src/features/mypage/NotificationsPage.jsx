import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyNotifications, markNotificationAsRead, markAllNotificationsAsRead, updateFcmToken } from './api';
import { isPushConfigured, requestPushToken } from './firebaseMessaging';
import { useToast } from '../../hooks/useToast';
import Button from '../../components/Button/Button';
import Pagination from '../../components/Pagination/Pagination';
import styles from './NotificationsPage.module.scss';

// type에 따라 클릭 시 이동할 경로가 다르다 (Header.jsx의 알림 드롭다운과 동일한 매핑)
const TYPE_TARGET_PATH = {
  COMMENT: (targetId) => `/boards/${targetId}`,
  REPLY: (targetId) => `/boards/${targetId}`,
  LIKE: (targetId) => `/boards/${targetId}`,
  BOOKMARK: (targetId) => `/boards/${targetId}`,
  TRAVEL: (targetId) => `/travels/${targetId}`,
};

export default function NotificationsPage() {
  const [page, setPage] = useState(0);
  const [notificationPage, setNotificationPage] = useState(null);
  const [isEnablingPush, setIsEnablingPush] = useState(false);
  const navigate = useNavigate();
  const { showToast } = useToast();

  async function load() {
    const res = await getMyNotifications({ page });
    setNotificationPage(res.data.data);
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  async function handleClick(notification) {
    if (!notification.read) {
      await markNotificationAsRead(notification.notificationId);
    }
    const pathFn = TYPE_TARGET_PATH[notification.type];
    navigate(pathFn ? pathFn(notification.targetId) : '/');
  }

  async function handleReadAll() {
    await markAllNotificationsAsRead();
    load();
  }

  async function handleEnablePush() {
    setIsEnablingPush(true);
    try {
      const token = await requestPushToken();
      if (!token) {
        showToast('알림 권한이 거부되었거나 이 브라우저에서 지원하지 않습니다.', 'error');
        return;
      }
      await updateFcmToken(token);
      showToast('브라우저 푸시 알림이 설정되었습니다.', 'success');
    } catch {
      showToast('푸시 알림 설정에 실패했습니다.', 'error');
    } finally {
      setIsEnablingPush(false);
    }
  }

  return (
    <div>
      <div className={styles.header}>
        <h1>알림</h1>
        <div className={styles.headerActions}>
          {isPushConfigured() && (
            <Button variant="secondary" disabled={isEnablingPush} onClick={handleEnablePush}>
              🔔 브라우저 푸시 알림 켜기
            </Button>
          )}
          <Button variant="secondary" onClick={handleReadAll}>
            전체 읽음 처리
          </Button>
        </div>
      </div>

      {notificationPage && (
        <>
          <ul className={styles.list}>
            {notificationPage.content.map((notification) => (
              <li key={notification.notificationId}>
                <button
                  className={`${styles.item} ${!notification.read ? styles.unread : ''}`}
                  onClick={() => handleClick(notification)}
                >
                  <span className={styles.title}>{notification.content}</span>
                  <span className={styles.meta}>{new Date(notification.createdAt).toLocaleString()}</span>
                </button>
              </li>
            ))}
            {notificationPage.content.length === 0 && <p className={styles.empty}>알림이 없습니다.</p>}
          </ul>
          <Pagination
            page={notificationPage.number}
            totalPages={notificationPage.totalPages}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  );
}
