import { useCallback, useRef, useState } from 'react';
import { ToastContext } from '../../hooks/useToast';
import styles from './Toast.module.scss';

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null);
  const timeoutIdRef = useRef(null);

  const showToast = useCallback((message, type = 'info') => {
    const id = Date.now();
    if (timeoutIdRef.current) {
      clearTimeout(timeoutIdRef.current);
    }
    setToast({ id, message, type });
    timeoutIdRef.current = setTimeout(() => {
      setToast((prev) => (prev?.id === id ? null : prev));
    }, 1500);
  }, []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className={styles.container}>
        {toast && (
          <div className={`${styles.toast} ${styles[toast.type]}`}>
            {toast.message}
          </div>
        )}
      </div>
    </ToastContext.Provider>
  );
}
