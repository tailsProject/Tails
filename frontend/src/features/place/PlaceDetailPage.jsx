// 독립 라우트용 장소 상세 페이지, 실제 내용은 PlaceDetailContent에 위임
import { useParams } from 'react-router-dom';
import PlaceDetailContent from './PlaceDetailContent';

export default function PlaceDetailPage() {
  const { placeId } = useParams();
  return <PlaceDetailContent placeId={placeId} />;
}
