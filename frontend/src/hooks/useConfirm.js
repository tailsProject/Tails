// 확인창 호출용 훅
import { createContext, useContext } from 'react';

export const ConfirmContext = createContext(null);

export function useConfirm() {
  const confirm = useContext(ConfirmContext);
  if (!confirm) {
    throw new Error('useConfirm은 ConfirmProvider 안에서만 사용할 수 있습니다.');
  }
  return confirm;
}
