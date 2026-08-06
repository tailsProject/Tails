// 브라우저 기본 confirm 대체용 확인창, Promise로 결과 전달
import { useCallback, useRef, useState } from 'react';
import Modal from './Modal';
import Button from '../Button/Button';
import { ConfirmContext } from '../../hooks/useConfirm';
import styles from './ConfirmModal.module.scss';

export function ConfirmProvider({ children }) {
  const [message, setMessage] = useState(null);
  const resolveRef = useRef(null);

  // 호출부는 await confirm(메시지) 형태로 사용, 버튼 클릭 시 Promise 해결
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
