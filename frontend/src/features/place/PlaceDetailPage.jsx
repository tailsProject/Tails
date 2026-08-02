// 장소 상세 - 리뷰 섹션을 붙일 최소 뼈대 화면(박영준의 Place 지도가 아직 없어서 임시로 구현).
// 박영준 쪽 실제 장소 상세 화면이 나오면 ReviewSection만 옮겨 붙이면 됨
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getPlaceDetail } from './api';
import ReviewSection from './ReviewSection';
import StateMessage from '../../components/StateMessage/StateMessage';
import { MapPinIcon } from '../../components/Icon/Icon';
import styles from './PlaceDetailPage.module.scss';

export default function PlaceDetailPage() {
  const { placeId } = useParams();
  const [place, setPlace] = useState(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    getPlaceDetail(placeId)
      .then((res) => setPlace(res.data.data))
      .catch(() => setNotFound(true));
  }, [placeId]);

  if (notFound) {
    return (
      <StateMessage
        icon={MapPinIcon}
        title="장소를 찾을 수 없어요"
        description="삭제되었거나 존재하지 않는 장소예요."
        actionTo="/"
        actionLabel="홈으로"
      />
    );
  }

  if (!place) {
    return null;
  }

  return (
    <div className={styles.wrapper}>
      <h1>{place.placeName}</h1>
      <p className={styles.address}>{place.address}</p>

      <ReviewSection placeId={placeId} />
    </div>
  );
}
