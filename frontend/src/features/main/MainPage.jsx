// 메인페이지, 히어로와 인기 장소/게시글/최근 리뷰 요약 담당
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { getBoards } from '../board/api';
import {
  getPlaceRatingSummaries,
  getPlaces,
  getPlacesRankedByPopular,
  getRecentReviews,
  getRecommendations,
  togglePlaceBookmark,
} from '../place/api';
import { resolveImage } from '../../utils/resolveImage';
import { getCategoryIconUrl } from '../../utils/placeCategory';
import { MapIcon, SuitcaseIcon, HeartIcon, FireIcon, ChatBubbleIcon, EyeIcon } from '../../components/Icon/Icon';
import styles from './MainPage.module.scss';

const ENTRY_LINKS = [
  { to: '/places', label: '지도', description: '반려동물 동반 가능한 장소를 찾아보세요', icon: MapIcon },
  { to: '/travels', label: '여행일정', description: '나만의 여행 일정을 만들어보세요', icon: SuitcaseIcon },
];

export default function MainPage() {
  const { isAuthenticated } = useAuth();
  const { showToast } = useToast();
  const [recentBoards, setRecentBoards] = useState([]);
  const [recentReviews, setRecentReviews] = useState([]);
  const [popularPlaces, setPopularPlaces] = useState([]);
  const [likedPlaceIds, setLikedPlaceIds] = useState(() => new Set());

  useEffect(() => {
    getBoards({ size: 3, sortBy: 'popular' }).then((res) => setRecentBoards(res.data.data.content));
    getRecentReviews({ size: 4 }).then((res) => setRecentReviews(res.data.data));
  }, []);

  useEffect(() => {
    // 로그인 상태면 개인화 추천을 먼저 시도, 추천이 없거나 비로그인이면 인기순으로 대체
    async function loadPlaceSection() {
      let places = [];

      if (isAuthenticated) {
        const recommended = await getRecommendations()
          .then((res) => res.data.data.map((r) => r.place))
          .catch(() => []);
        if (recommended.length > 0) {
          places = recommended.slice(0, 4);
        }
      }

      if (places.length === 0) {
        places = await getPlacesRankedByPopular({ size: 4 }).then((res) => res.data.data.content);
      }

      // 찜이 하나도 없으면 인기순 랭킹 자체가 비어있으므로, 최소한 등록된 장소는 보이도록 대체
      if (places.length === 0) {
        places = await getPlaces({ size: 4 }).then((res) => res.data.data.content);
      }

      const ratingSummaries = await getPlaceRatingSummaries(places.map((place) => place.placeId))
        .then((res) => new Map(res.data.data.map((r) => [r.placeId, r])))
        .catch(() => new Map());

      setPopularPlaces(
        places.map((place) => {
          const summary = ratingSummaries.get(place.placeId);
          return summary ? { ...place, averageRating: summary.averageRating, reviewCount: summary.reviewCount } : place;
        }),
      );
    }

    loadPlaceSection();
  }, [isAuthenticated]);

  async function handleToggleBookmark(e, placeId) {
    e.preventDefault();
    e.stopPropagation();
    if (!isAuthenticated) {
      showToast('로그인이 필요합니다.', 'error');
      return;
    }
    try {
      const res = await togglePlaceBookmark(placeId);
      setLikedPlaceIds((prev) => {
        const next = new Set(prev);
        if (res.data.data.bookmarked) {
          next.add(placeId);
        } else {
          next.delete(placeId);
        }
        return next;
      });
    } catch (error) {
      showToast(error.response?.data?.error?.message ?? '찜 처리에 실패했습니다.', 'error');
    }
  }

  return (
    <div className={styles.main}>
      <section className={styles.hero}>
        <div className={styles.heroInner}>
          <h1>반려동물과 함께하는 여행, Tails</h1>
          <p>동반 가능한 장소를 찾고, 일정을 짜고, 경험을 나눠보세요.</p>
        </div>
      </section>

      <section className={styles.links}>
        {ENTRY_LINKS.map((link) => (
          <Link key={link.to} to={link.to} className={styles.card}>
            <span className={styles.cardIcon}>
              <link.icon />
            </span>
            <h2>{link.label}</h2>
            <p>{link.description}</p>
          </Link>
        ))}
      </section>

      {popularPlaces.length > 0 && (
        <section className={styles.section}>
          <div className={styles.sectionHeader}>
            <h2>지금 인기있는 장소</h2>
            <Link to="/places" className={styles.moreLink}>
              전체보기 →
            </Link>
          </div>
          <div className={styles.placeScroll}>
            {popularPlaces.map((place) => (
              <Link key={place.placeId} to={`/places/${place.placeId}`} className={styles.placeCard}>
                <div className={styles.placeImageWrap}>
                  {resolveImage(place.imageUrl) ? (
                    <img src={resolveImage(place.imageUrl)} alt={place.placeName} />
                  ) : (
                    <div className={styles.placeImageFallback}>
                      <img src={getCategoryIconUrl(place)} alt="" />
                    </div>
                  )}
                  <button
                    type="button"
                    className={`${styles.likeButton} ${likedPlaceIds.has(place.placeId) ? styles.liked : ''}`}
                    onClick={(e) => handleToggleBookmark(e, place.placeId)}
                    aria-label={likedPlaceIds.has(place.placeId) ? `${place.placeName} 찜 해제` : `${place.placeName} 찜하기`}
                  >
                    <HeartIcon fill={likedPlaceIds.has(place.placeId) ? 'currentColor' : 'none'} />
                  </button>
                </div>
                <div className={styles.placeBody}>
                  <p className={styles.placeName}>{place.placeName}</p>
                  <p className={styles.placeAddress}>{place.address}</p>
                  {place.reviewCount > 0 && (
                    <div className={styles.placeRating}>
                      <span className={styles.ratingValue}>★ {place.averageRating.toFixed(1)}</span>
                      <span className={styles.placeReviewCount}>리뷰 {place.reviewCount}</span>
                    </div>
                  )}
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {(recentBoards.length > 0 || recentReviews.length > 0) && (
        <section className={styles.section}>
          <div className={styles.recentGrid}>
            {recentBoards.length > 0 && (
              <div className={styles.recentColumn}>
                <div className={styles.sectionHeader}>
                  <h2>
                    <FireIcon /> 인기 이야기
                  </h2>
                  <Link to="/boards" className={styles.moreLink}>
                    피드로 가기 →
                  </Link>
                </div>
                <ul className={styles.boardList}>
                  {recentBoards.map((board) => (
                    <li key={board.boardId}>
                      <Link to={`/boards/${board.boardId}`} className={styles.boardItem}>
                        <h3>{board.title}</h3>
                        {board.excerpt && <p>{board.excerpt}</p>}
                        <div className={styles.boardMeta}>
                          <span>{board.authorNickname}</span>
                          <span>
                            <EyeIcon /> {board.viewCount}
                          </span>
                          <span>
                            <HeartIcon /> {board.likeCount}
                          </span>
                          <span>
                            <ChatBubbleIcon /> {board.commentCount}
                          </span>
                        </div>
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {recentReviews.length > 0 && (
              <div className={styles.recentColumn}>
                <div className={styles.sectionHeader}>
                  <h2>최근 리뷰</h2>
                  <Link to="/places" className={styles.moreLink}>
                    지도보기 →
                  </Link>
                </div>
                <ul className={styles.reviewList}>
                  {recentReviews.map((review) => (
                    <li key={review.reviewId}>
                      <Link to={`/places/${review.placeId}`} className={styles.reviewItem}>
                        <p className={styles.reviewContent}>{review.content}</p>
                        <div className={styles.reviewMeta}>
                          <span>{review.authorNickname}</span>
                          <span className={styles.ratingValue}>★ {review.rating}</span>
                          <span>{review.placeName}</span>
                        </div>
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </section>
      )}
    </div>
  );
}
