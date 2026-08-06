import { createContext, useContext } from 'react';

export const NotificationContext = createContext(null);

export function useNotifications() {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications는 NotificationProvider 안에서만 사용할 수 있습니다.');
  }
  return context;
}
