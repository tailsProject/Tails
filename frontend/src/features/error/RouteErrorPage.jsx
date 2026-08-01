// 라우트 렌더링 중 예외 발생 시 노출되는 에러 페이지
import { useRouteError } from 'react-router-dom';
import { WarningIcon } from '../../components/Icon/Icon';
import StateMessage from '../../components/StateMessage/StateMessage';

export default function RouteErrorPage() {
  const error = useRouteError();
  if (import.meta.env.DEV) {
    console.error(error);
  }

  return (
    <StateMessage
      icon={WarningIcon}
      title="문제가 발생했어요"
      description="일시적인 오류일 수 있어요. 새로고침해도 계속되면 잠시 후 다시 시도해주세요."
      onAction={() => window.location.reload()}
      actionLabel="새로고침"
    />
  );
}
