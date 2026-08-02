import { useEffect, useState } from 'react';
import { createTravel, updateTravel, getTravelDetail } from './api';
import { useToast } from '../../hooks/useToast';
import Modal from '../../components/Modal/Modal';
import Button from '../../components/Button/Button';
import styles from './TravelFormModal.module.scss';

const EMPTY_FORM = { title: '', startDate: '', endDate: '' };

// 여행 만들기/수정 공용 모달
export default function TravelFormModal({ open, onClose, travelId, onSaved }) {
  const isEdit = Boolean(travelId);
  const { showToast } = useToast();
  const [form, setForm] = useState(EMPTY_FORM);
  const [loaded, setLoaded] = useState(!isEdit);

  useEffect(() => {
    if (!open) return;

    if (isEdit) {
      getTravelDetail(travelId).then((res) => {
        const t = res.data.data;
        setForm({ title: t.title, startDate: t.startDate, endDate: t.endDate });
        setLoaded(true);
      });
    } else {
      setForm(EMPTY_FORM);
      setLoaded(true);
    }
  }, [open, travelId, isEdit]);

  async function handleSubmit(e) {
    e.preventDefault();
    try {
      if (isEdit) {
        await updateTravel(travelId, form);
        onSaved(travelId);
      } else {
        const res = await createTravel(form);
        onSaved(res.data.data.travelId);
      }
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '저장에 실패했습니다.', 'error');
    }
  }

  return (
    <Modal open={open} onClose={onClose}>
      <div className={styles.modal}>
        <h2>{isEdit ? '여행 정보 수정' : '새 여행 만들기'}</h2>

        {loaded && (
          <form onSubmit={handleSubmit} className={styles.form}>
            <label>
              <span className={styles.fieldLabel}>여행 제목</span>
              <input
                type="text"
                placeholder="예: 제주도 반려견 여행"
                value={form.title}
                onChange={(e) => setForm((prev) => ({ ...prev, title: e.target.value }))}
                maxLength={255}
                required
                autoFocus
              />
            </label>

            <div className={styles.dateRow}>
              <label>
                <span className={styles.fieldLabel}>시작일</span>
                <input
                  type="date"
                  value={form.startDate}
                  onChange={(e) => setForm((prev) => ({ ...prev, startDate: e.target.value }))}
                  required
                />
              </label>
              <label>
                <span className={styles.fieldLabel}>종료일</span>
                <input
                  type="date"
                  value={form.endDate}
                  onChange={(e) => setForm((prev) => ({ ...prev, endDate: e.target.value }))}
                  required
                />
              </label>
            </div>

            <div className={styles.actions}>
              <Button type="button" variant="secondary" onClick={onClose}>
                취소
              </Button>
              <Button type="submit">{isEdit ? '저장' : '만들기'}</Button>
            </div>
          </form>
        )}
      </div>
    </Modal>
  );
}
