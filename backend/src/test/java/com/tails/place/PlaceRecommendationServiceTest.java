package com.tails.place;

import com.tails.bookmark.PlaceBookmarkRepository;
import com.tails.place.dto.PlaceRecommendationResponse;
import com.tails.review.Review;
import com.tails.review.ReviewRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// PlaceRecommendationService.recommend() 콘텐츠 기반(코사인 유사도) 추천 회귀테스트
@ExtendWith(MockitoExtension.class)
class PlaceRecommendationServiceTest {

    @Mock
    private PlaceBookmarkRepository placeBookmarkRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private PlaceRecommendationService placeRecommendationService;

    private Place newPlace(Long placeId, String cat1) {
        Place place = Place.builder().externalPlaceId("ext" + placeId).placeName("place" + placeId).cat1(cat1).build();
        ReflectionTestUtils.setField(place, "placeId", placeId);
        return place;
    }

    @Test
    void 찜이나_리뷰_이력이_없으면_빈_목록을_반환한다() {
        when(placeBookmarkRepository.findBookmarkedPlacesByMemberId(any(), any(Pageable.class))).thenReturn(Page.empty());
        when(reviewRepository.findByMemberIdWithPlace(any(), any(Pageable.class))).thenReturn(Page.empty());

        List<PlaceRecommendationResponse> result = placeRecommendationService.recommend(1L);

        assertThat(result).isEmpty();
        verify(placeRepository, never()).findAll();
    }

    @Test
    void 카테고리가_겹치지_않는_장소는_추천에서_제외된다() {
        Place bookmarked = newPlace(1L, "cafe");
        Place similar = newPlace(2L, "cafe");
        Place unrelated = newPlace(3L, "park");
        when(placeBookmarkRepository.findBookmarkedPlacesByMemberId(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(bookmarked)));
        when(reviewRepository.findByMemberIdWithPlace(any(), any(Pageable.class))).thenReturn(Page.empty());
        when(placeRepository.findAll()).thenReturn(List.of(bookmarked, similar, unrelated));

        List<PlaceRecommendationResponse> result = placeRecommendationService.recommend(1L);

        assertThat(result).extracting(r -> r.place().getPlaceId()).containsExactly(2L);
    }

    @Test
    void 이미_찜하거나_리뷰를_남긴_장소는_추천에서_제외된다() {
        Place bookmarked = newPlace(1L, "cafe");
        Place reviewed = newPlace(2L, "cafe");
        Place candidate = newPlace(3L, "cafe");
        Review review = Review.builder().place(reviewed).rating(5).content("c").build();
        when(placeBookmarkRepository.findBookmarkedPlacesByMemberId(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(bookmarked)));
        when(reviewRepository.findByMemberIdWithPlace(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(review)));
        when(placeRepository.findAll()).thenReturn(List.of(bookmarked, reviewed, candidate));

        List<PlaceRecommendationResponse> result = placeRecommendationService.recommend(1L);

        assertThat(result).extracting(r -> r.place().getPlaceId()).containsExactly(3L);
    }
}
