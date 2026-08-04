// 전체 알림 목록 페이지, 웹 푸시 켜기 포함
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyNotifications, markNotificationAsRead, markAllNotificationsAsRead, deleteAllNotifications, updateFcmToken } from './api';
import { isPushConfigured, requestPushToken } from './firebaseMessaging';
import { useToast } from '../../hooks/useToast';
import { useConfirm } from '../../hooks/useConfirm';
import { useNotifications } from '../../hooks/useNotifications';
import { BellIcon, ChatBubbleIcon, HeartIcon, BookmarkIcon, SuitcaseIcon, CheckIcon, TrashIcon } from '../../components/Icon/Icon';
import Pagination from '../../components/Pagination/Pagination';
import styles from './ListPage.module.scss';

const TYPE_TARGET_PATH = {
  COMMENT: (targetId) => `/boards/${targetId}`,
  REPLY: (targetId) => `/boards/${targetId}`,
  LIKE: (targetId) => `/boards/${targetId}`,
  BOOKMARK: (targetId) => `/boards/${targetId}`,
  TRAVEL: (targetId) => `/travels/${targetId}`,
  REPORT_RESOLVED: () => '/mypage/reports',
};

const TYPE_ICON = {
  COMMENT: ChatBubbleIcon,
  REPLY: ChatBubbleIcon,
  LIKE: HeartIcon,
  BOOKMARK: BookmarkIcon,
  TRAVEL: SuitcaseIcon,
  REPORT_RESOLVED: CheckIcon,
};

export default function NotificationsPage() {
  const [page, setPage] = useState(0);
  const [notificationPage, setNotificationPage] = useState(null);
  const [isEnablingPush, setIsEnablingPush] = useState(false);
  const navigate = useNavigate();
  const { showToast } = useToast();
  const confirm = useConfirm();
  const { refresh: refreshNotifications } = useNotifications();

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
      refreshNotifications();
    }
    const pathFn = TYPE_TARGET_PATH[notification.type];
    navigate(pathFn ? pathFn(notification.targetId) : '/');
  }

  async function handleReadAll() {
    await markAllNotificationsAsRead();
    refreshNotifications();
    load();
  }

  async function handleDeleteAll() {
    const ok = await confirm('알림을 전부 삭제하시겠습니까?');
    if (!ok) return;
    await deleteAllNotifications();
    refreshNotifications();
    if (page === 0) {
      load();
    } else {
      setPage(0);
    }
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
            <button
              type="button"
              className={styles.iconActionBtn}
              disabled={isEnablingPush}
              onClick={handleEnablePush}
              aria-label="푸시 알림 켜기"
              data-tooltip="푸시 알림 켜기"
            >
              <BellIcon />
            </button>
          )}
          <button
            type="button"
            className={styles.iconActionBtn}
            onClick={handleReadAll}
            aria-label="전체 읽음"
            data-tooltip="전체 읽음"
          >
            <CheckIcon />
          </button>
          <button
            type="button"
            className={styles.iconActionBtnDanger}
            onClick={handleDeleteAll}
            aria-label="전체 삭제"
            data-tooltip="전체 삭제"
          >
            <TrashIcon />
          </button>
        </div>
      </div>

      {notificationPage && (
        <>
          <ul className={styles.list}>
            {notificationPage.content.map((notification) => {
              const TypeIcon = TYPE_ICON[notification.type] ?? BellIcon;
              return (
                <li key={notification.notificationId}>
                  <button
                    className={`${styles.item} ${!notification.read ? styles.unread : styles.read}`}
                    onClick={() => handleClick(notification)}
                  >
                    <span className={styles.itemThumbWrap}>
                      <span className={styles.itemThumb}>
                        <TypeIcon />
                      </span>
                      {!notification.read && <span className={styles.unreadDot} />}
                    </span>
                    <span className={styles.itemBody}>
                      <span className={styles.title}>{notification.content}</span>
                      <span className={styles.metaDate}>{new Date(notification.createdAt).toLocaleString()}</span>
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>
          {notificationPage.content.length === 0 && (
            <div className={styles.empty}>
              <span className={styles.emptyIcon}>
                <BellIcon />
              </span>
              <p>알림이 없습니다.</p>
            </div>
          )}
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
