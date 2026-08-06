// 공통 모달 껍데기, body에 포털로 렌더링
import { createPortal } from 'react-dom';
import styles from './Modal.module.scss';

export default function Modal({ open, onClose, children }) {
  if (!open) {
    return null;
  }

  return createPortal(
    <div className={styles.backdrop} onClick={onClose}>
      <div className={styles.content} onClick={(e) => e.stopPropagation()}>
        {children}
      </div>
    </div>,
    document.body,
  );
}
