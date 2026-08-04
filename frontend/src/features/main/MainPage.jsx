// 메인페이지, 히어로 검색과 인기 장소/피드/리뷰 요약 담당
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { getBoards } from '../board/api';
import { getPlaceRatingSummaries, getPlaces, getPlacesRankedByPopular, getRecentReviews, getRecommendations, togglePlaceBookmark } from '../place/api';
import { getMyBookmarkedPlaces } from '../mypage/api';
import { resolveImage, resolveProfileImage } from '../../utils/resolveImage';
import { timeAgo } from '../../utils/timeAgo';
import { CATEGORIES, getCategoryIconUrl } from '../../utils/placeCategory';
import { PawIcon, MagnifyingGlassIcon, HeartIcon, FireIcon, ChatBubbleIcon, EyeIcon } from '../../components/Icon/Icon';
import styles from './MainPage.module.scss';

const FEED_SECTION_SIZE = 5;

const HERO_CATEGORY_CHIPS = CATEGORIES.filter((c) => c.key);

const HERO_SLIDE_INTERVAL = 4000;

const HERO_SLIDES = [
  {
    src: 'https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=1600&h=1200&fit=crop&auto=format',
    alt: '웃고 있는 보더콜리',
  },
  {
    src: 'https://images.unsplash.com/photo-1552053831-71594a27632d?w=1600&h=1200&fit=crop&auto=format',
    alt: '꽃을 물고 있는 리트리버',
  },
  {
    src: 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=1600&h=1200&fit=crop&auto=format',
    alt: '장난감을 가지고 노는 복서 믹스',
  },
  {
    src: 'https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=1600&h=1200&fit=crop&auto=format',
    alt: '선글라스를 낀 검은 브리티시 블루',
  },
  {
    src: 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=1600&h=1200&fit=crop&auto=format',
    alt: '근엄하게 앉아 있는 코숏',
  },
];

export default function MainPage() {
  const navigate = useNavigate();
  const { isAuthenticated, member } = useAuth();
  const { showToast } = useToast();
  const [heroKeyword, setHeroKeyword] = useState('');
  const [recentBoards, setRecentBoards] = useState([]);
  const [recentReviews, setRecentReviews] = useState([]);
  const [popularPlaces, setPopularPlaces] = useState([]);
  const [placeSectionTitle, setPlaceSectionTitle] = useState('지금 인기있는 장소');
  const [placeTotal, setPlaceTotal] = useState(null);
  const [boardTotal, setBoardTotal] = useState(null);
  const [likedPlaceIds, setLikedPlaceIds] = useState(() => new Set());
  const [heroSlideIndex, setHeroSlideIndex] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => {
      setHeroSlideIndex((prev) => (prev + 1) % HERO_SLIDES.length);
    }, HERO_SLIDE_INTERVAL);
    return () => clearTimeout(timer);
  }, [heroSlideIndex]);

  useEffect(() => {
    if (!isAuthenticated) {
      setLikedPlaceIds(new Set());
      return;
    }
    getMyBookmarkedPlaces({ size: 100 }).then((res) => {
      setLikedPlaceIds(new Set(res.data.data.content.map((place) => place.placeId)));
    });
  }, [isAuthenticated]);

  useEffect(() => {
    getBoards({ size: FEED_SECTION_SIZE, sortBy: 'popular' }).then((res) => {
      setRecentBoards(res.data.data.content);
      setBoardTotal(res.data.data.totalElements);
    });
    getPlaces({ size: 1 }).then((res) => setPlaceTotal(res.data.data.totalElements));
    getRecentReviews({ size: 4 }).then((res) => setRecentReviews(res.data.data));
  }, []);

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

  useEffect(() => {
    // 로그인 상태면 개인화 추천을 먼저 시도, 추천이 없거나 비로그인이면 인기순으로 대체
    async function loadPlaceSection() {
      let places = [];
      let usedRecommendation = false;

      if (isAuthenticated) {
        const recommended = await getRecommendations()
          .then((res) => res.data.data.map((r) => r.place))
          .catch(() => []);
        if (recommended.length > 0) {
          places = recommended.slice(0, 4);
          usedRecommendation = true;
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

      setPopularPlaces(places.map((place) => {
        const summary = ratingSummaries.get(place.placeId);
        return summary ? { ...place, averageRating: summary.averageRating, reviewCount: summary.reviewCount } : place;
      }));
      setPlaceSectionTitle(usedRecommendation ? `${member.nickname}님을 위한 추천 장소` : '지금 인기있는 장소');
    }

    loadPlaceSection();
  }, [isAuthenticated]);

  function handleHeroSearchSubmit(e) {
    e.preventDefault();
    const trimmed = heroKeyword.trim();
    navigate(trimmed ? `/places?keyword=${encodeURIComponent(trimmed)}` : '/places');
  }

  return (
    <>
      <section className={styles.hero}>
        <div className={styles.heroInner}>
        <div className={styles.heroCopy}>
          <span className={styles.heroBadge}><PawIcon /> 반려동물 동반 여행 플랫폼</span>
          <h1>
            반려동물과 함께,
            <br />
            더 특별한 여행을
            <br />
            <span className={styles.heroAccent}>떠나요</span>
          </h1>
          <p>전국 반려동물 동반 장소를 지도에서 찾고, 여행 일정을 계획하고, 소중한 추억을 나눠보세요.</p>

          <form className={styles.heroSearch} role="search" onSubmit={handleHeroSearchSubmit}>
            <span className={styles.heroSearchIcon} aria-hidden="true"><MagnifyingGlassIcon /></span>
            <input
              aria-label="장소 검색"
              placeholder="지역, 장소 이름으로 검색"
              value={heroKeyword}
              onChange={(e) => setHeroKeyword(e.target.value)}
            />
            <button type="submit" className={styles.heroSearchButton}>검색</button>
          </form>

          <div className={styles.heroChips}>
            {HERO_CATEGORY_CHIPS.map((c) => (
              <Link key={c.key} to={`/places?category=${c.key}`} className={styles.heroChip}>
                {c.label}
              </Link>
            ))}
          </div>

          {(placeTotal != null || boardTotal != null) && (
            <div className={styles.heroStats}>
              {placeTotal != null && (
                <div>
                  <span className={styles.heroStatValue}>{placeTotal.toLocaleString()}+</span>
                  <span className={styles.heroStatLabel}>동반 장소</span>
                </div>
              )}
              {boardTotal != null && (
                <div>
                  <span className={styles.heroStatValue}>{boardTotal.toLocaleString()}+</span>
                  <span className={styles.heroStatLabel}>여행 이야기</span>
                </div>
              )}
            </div>
          )}
        </div>

        <div className={styles.heroMedia}>
          {HERO_SLIDES.map((slide, index) => (
            <img
              key={slide.src}
              src={slide.src}
              alt={slide.alt}
              className={index === heroSlideIndex ? styles.heroMediaActive : undefined}
            />
          ))}
          <div className={styles.heroMediaDots}>
            {HERO_SLIDES.map((slide, index) => (
              <button
                key={slide.src}
                type="button"
                aria-label={`${index + 1}번째 사진 보기`}
                className={index === heroSlideIndex ? styles.heroMediaDotActive : styles.heroMediaDot}
                onClick={() => setHeroSlideIndex(index)}
              />
            ))}
          </div>
          {placeTotal != null && (
            <div className={styles.heroMediaCard}>
              <span className={styles.heroMediaNum}>{placeTotal.toLocaleString()}+</span>
              <div>
                <strong>동반 가능 장소</strong>
                <span>카페 · 식당 · 공원 · 숙소</span>
              </div>
            </div>
          )}
        </div>
        </div>
      </section>

      <div className={styles.main}>
      {popularPlaces.length > 0 && (
        <section className={styles.section}>
          <div className={styles.sectionHeader}>
            <h2>{placeSectionTitle}</h2>
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
                    aria-pressed={likedPlaceIds.has(place.placeId)}
                  >
                    <HeartIcon fill={likedPlaceIds.has(place.placeId) ? 'currentColor' : 'none'} />
                  </button>
                  {place.bookmarkCount != null && (
                    <span className={styles.placeBookmark}><HeartIcon /> {place.bookmarkCount}</span>
                  )}
                </div>
                <div className={styles.placeBody}>
                  <p className={styles.placeName}>{place.placeName}</p>
                  <p className={styles.placeAddress}>{place.address}</p>
                  {place.reviewCount > 0 && (
                    <div className={styles.placeRating}>
                      <span className={styles.ratingStars}>
                        {Array.from({ length: 5 }, (_, i) => (
                          <span
                            key={i}
                            className={i < Math.round(place.averageRating) ? styles.starFilled : styles.starEmpty}
                          >
                            ★
                          </span>
                        ))}
                      </span>
                      <span className={styles.placeRatingValue}>{place.averageRating.toFixed(1)}</span>
                      <span className={styles.placeReviewCount}>리뷰 {place.reviewCount}</span>
                    </div>
                  )}
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {recentBoards.length > 0 && (
        <section className={styles.section}>
          <div className={styles.sectionHeader}>
            <div>
              <h2>인기 이야기</h2>
            </div>
            <Link to="/boards" className={styles.moreLink}>
              피드로 가기 →
            </Link>
          </div>

          <div className={styles.feedRow}>
            <Link to={`/boards/${recentBoards[0].boardId}`} className={styles.feedBig}>
              {resolveImage(recentBoards[0].thumbnailUrl) && (
                <img src={resolveImage(recentBoards[0].thumbnailUrl)} alt="" />
              )}
              <div className={styles.feedBigBody}>
                <span className={styles.feedBadge}><FireIcon /> 인기글</span>
                <h3>{recentBoards[0].title}</h3>
                {recentBoards[0].excerpt && <p>{recentBoards[0].excerpt}</p>}
                <div className={styles.feedBigFooter}>
                  <span className={styles.feedAvatar}>
                    <img src={resolveProfileImage(recentBoards[0].authorProfileImg)} alt="" />
                  </span>
                  <span>{recentBoards[0].authorNickname}</span>
                  <span><EyeIcon /> {recentBoards[0].viewCount}</span>
                  <span><HeartIcon /> {recentBoards[0].likeCount}</span>
                  <span><ChatBubbleIcon /> {recentBoards[0].commentCount}</span>
                </div>
              </div>
            </Link>

            {recentBoards.length > 1 && (
              <div className={styles.feedGrid}>
                {recentBoards.slice(1).map((board) => (
                  <Link key={board.boardId} to={`/boards/${board.boardId}`} className={styles.feedItem}>
                    <h3>{board.title}</h3>
                    {board.excerpt && <p className={styles.feedExcerpt}>{board.excerpt}</p>}
                    <div className={styles.feedFooter}>
                      <span className={styles.feedAvatar}>
                        <img src={resolveProfileImage(board.authorProfileImg)} alt="" />
                      </span>
                      <span>{board.authorNickname}</span>
                      <span><EyeIcon /> {board.viewCount}</span>
                      <span><HeartIcon /> {board.likeCount}</span>
                      <span><ChatBubbleIcon /> {board.commentCount}</span>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </div>
        </section>
      )}

      {recentReviews.length > 0 && (
        <section className={styles.section}>
          <div className={styles.recentCard}>
            <div className={styles.recentCardHeader}>
              <h2>최근 리뷰</h2>
              <Link to="/places" className={styles.moreLink}>
                지도보기 →
              </Link>
            </div>
            <ul className={styles.recentList}>
              {recentReviews.map((review) => (
                <li key={review.reviewId}>
                  <Link to={`/places/${review.placeId}`} className={styles.recentItem}>
                    <p className={styles.recentTitle}>{review.content}</p>
                    <div className={styles.recentMeta}>
                      <span className={styles.recentAvatar}>
                        <img src={resolveProfileImage(review.authorProfileImg)} alt="" />
                      </span>
                      <span>{review.authorNickname}</span>
                      <span className={styles.ratingStars}>
                        {Array.from({ length: 5 }, (_, i) => (
                          <span key={i} className={i < review.rating ? styles.starFilled : styles.starEmpty}>
                            ★
                          </span>
                        ))}
                      </span>
                      <span>{review.placeName}</span>
                      <span className={styles.recentTime}>{timeAgo(review.createdAt)}</span>
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </section>
      )}
      </div>
    </>
  );
}
