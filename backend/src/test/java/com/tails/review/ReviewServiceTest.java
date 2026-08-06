package com.tails.review;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.util.FileStorage;
import com.tails.image.Image;
import com.tails.image.ImageRepository;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import com.tails.place.Place;
import com.tails.place.PlaceRepository;
import com.tails.review.dto.ReviewCreateRequest;
import com.tails.review.dto.ReviewUpdateRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ReviewService 단위테스트 - 중복 작성 방지/타 장소 reviewId 차단/작성자 확인/삭제 시 이미지 정리
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ImageRepository imageRepository;
    @Mock
    private FileStorage fileStorage;

    @InjectMocks
    private ReviewService reviewService;

    private Member newMember(Long id) {
        Member member = Member.builder().email(id + "@test.com").password("x").nickname("n" + id).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Place newPlace(Long placeId) {
        Place place = Place.builder().externalPlaceId("ext" + placeId).placeName("place" + placeId).build();
        ReflectionTestUtils.setField(place, "placeId", placeId);
        return place;
    }

    private Review newReview(Long reviewId, Member member, Place place) {
        Review review = Review.builder().member(member).place(place).rating(5).content("c").build();
        ReflectionTestUtils.setField(review, "reviewId", reviewId);
        return review;
    }

    @Test
    void 이미_리뷰를_작성한_장소에_또_작성하면_DUPLICATE_REVIEW() {
        Place place = newPlace(1L);
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));
        when(reviewRepository.existsByPlace_PlaceIdAndMember_Id(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.create(10L, 1L, new ReviewCreateRequest(5, "content")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_REVIEW);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void 다른_장소_소속인_reviewId로_수정하면_REVIEW_NOT_FOUND() {
        Place place1 = newPlace(1L);
        Place place2 = newPlace(2L);
        Review review = newReview(100L, newMember(10L), place2);
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.update(10L, 1L, 100L, new ReviewUpdateRequest(4, "edited")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    void 작성자가_아니면_수정할_수_없다_NOT_REVIEW_OWNER() {
        Place place = newPlace(1L);
        Review review = newReview(100L, newMember(10L), place);
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.update(99L, 1L, 100L, new ReviewUpdateRequest(4, "edited")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_REVIEW_OWNER);
    }

    @Test
    void 탈퇴한_회원의_리뷰는_아무도_수정할_수_없다() {
        Place place = newPlace(1L);
        Review review = newReview(100L, null, place);
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.update(10L, 1L, 100L, new ReviewUpdateRequest(4, "edited")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_REVIEW_OWNER);
    }

    @Test
    void 리뷰_삭제시_첨부된_이미지_파일도_함께_정리된다() {
        Place place = newPlace(1L);
        Member owner = newMember(10L);
        Review review = newReview(100L, owner, place);
        Image image1 = Image.builder().review(review).storedFileName("a.jpg").originalFileName("a.jpg").sequence(0).build();
        Image image2 = Image.builder().review(review).storedFileName("b.jpg").originalFileName("b.jpg").sequence(1).build();
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        when(imageRepository.findByReview_ReviewIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(image1, image2));

        reviewService.delete(10L, 1L, 100L);

        verify(fileStorage).deleteAfterCommit("a.jpg");
        verify(fileStorage).deleteAfterCommit("b.jpg");
        verify(reviewRepository).delete(review);
    }
}
