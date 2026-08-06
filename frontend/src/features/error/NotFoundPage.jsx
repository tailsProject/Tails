// 존재하지 않는 경로 접근 시 노출되는 404 페이지
import { PawIcon } from '../../components/Icon/Icon';
import StateMessage from '../../components/StateMessage/StateMessage';

export default function NotFoundPage() {
  return (
    <StateMessage
      icon={PawIcon}
      title="페이지를 찾을 수 없어요"
      description="주소가 잘못됐거나 삭제된 페이지예요."
      actionTo="/"
      actionLabel="홈으로 가기"
    />
  );
}
