import { useCallback, useRef, useState } from 'react';
import Modal from './Modal';
import Button from '../Button/Button';
import { ConfirmContext } from '../../hooks/useConfirm';
import styles from './ConfirmModal.module.scss';

export function ConfirmProvider({ children }) {
  const [message, setMessage] = useState(null);
  const resolveRef = useRef(null);

  const confirm = useCallback((msg) => {
    setMessage(msg);
    return new Promise((resolve) => {
      resolveRef.current = resolve;
    });
  }, []);

  function handleResult(result) {
    resolveRef.current?.(result);
    setMessage(null);
  }

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      <Modal open={message !== null} onClose={() => handleResult(false)}>
        <p className={styles.message}>{message}</p>
        <div className={styles.actions}>
          <Button variant="secondary" onClick={() => handleResult(false)}>
            취소
          </Button>
          <Button onClick={() => handleResult(true)}>확인</Button>
        </div>
      </Modal>
    </ConfirmContext.Provider>
  );
}
