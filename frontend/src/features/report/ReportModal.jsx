// 게시글, 댓글, 리뷰 공통 신고 모달
import { useState } from 'react';
import Modal from '../../components/Modal/Modal';
import Button from '../../components/Button/Button';
import { useToast } from '../../hooks/useToast';
import { createReport } from './api';
import styles from './ReportModal.module.scss';

export default function ReportModal({ open, onClose, targetType, targetId }) {
  const [reason, setReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { showToast } = useToast();

  async function handleSubmit(e) {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await createReport({ targetType, targetId, reason });
      showToast('신고가 접수되었습니다.', 'success');
      setReason('');
      onClose();
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '신고에 실패했습니다.', 'error');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose}>
      <h2>신고하기</h2>
      <form onSubmit={handleSubmit} className={styles.form}>
        <textarea
          placeholder="신고 사유를 입력해주세요"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          maxLength={500}
          rows={5}
          required
        />
        <div className={styles.actions}>
          <Button type="button" variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            신고 제출
          </Button>
        </div>
      </form>
    </Modal>
  );
}
