// 헤더 알림 뱃지, 알림 목록 전역 제공
import { useCallback, useEffect, useState } from 'react';
import { getMyNotifications } from '../features/mypage/api';
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
    // 실시간 push 대신 30초 간격 폴링으로 헤더 뱃지 갱신
    const intervalId = setInterval(refresh, 30000);
    return () => clearInterval(intervalId);
  }, [refresh, isAuthenticated]);

  const unreadCount = notifications.filter((n) => !n.read).length;

  const value = { notifications, unreadCount, refresh };

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}
