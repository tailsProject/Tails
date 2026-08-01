import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { NotificationContext } from '../hooks/useNotifications';

export function NotificationProvider({ children }) {
  const { isAuthenticated } = useAuth();
  const [notifications, setNotifications] = useState([]);

  const refresh = useCallback(async () => {
    if (!isAuthenticated) {
      setNotifications([]);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const unreadCount = notifications.filter((n) => !n.read).length;

  const value = { notifications, unreadCount, refresh };

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}
