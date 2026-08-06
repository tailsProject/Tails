// 헤더 알림 뱃지, 알림 목록 전역 제공
import { useCallback, useEffect, useState } from 'react';
import { getMyNotifications } from '../features/mypage/api';
import { subscribeToForegroundMessages } from '../features/mypage/firebaseMessaging';
import { useAuth } from '../hooks/useAuth';
import { NotificationContext } from '../hooks/useNotifications';

export function NotificationProvider({ children }) {
  const { isAuthenticated } = useAuth();
  const [notifications, setNotifications] = useState([]);

  const refresh = useCallback(async () => {
    if (!isAuthenticated) {
      setNotifications([]);
      return;
    }
    const res = await getMyNotifications({ page: 0, size: 5 });
    setNotifications(res.data.data.content);
  }, [isAuthenticated]);

  useEffect(() => {
    refresh();
    if (!isAuthenticated) return undefined;
    // 포그라운드로 FCM이 도착하면 즉시 갱신, 그 외엔 10초 폴링으로 보완
    const unsubscribe = subscribeToForegroundMessages(refresh);
    const intervalId = setInterval(refresh, 10000);
    return () => {
      unsubscribe();
      clearInterval(intervalId);
    };
  }, [refresh, isAuthenticated]);

  const unreadCount = notifications.filter((n) => !n.read).length;

  const value = { notifications, unreadCount, refresh };

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}
