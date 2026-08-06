package com.tails.place;

import com.tails.bookmark.PlaceBookmarkRepository;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.review.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

// PlaceService.searchPlaces() 검색 조건 자체의 유효성(좌표 일부만 옴/조건 전무) 회귀테스트
@ExtendWith(MockitoExtension.class)
class PlaceServiceSearchConditionTest {

    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private PlaceBookmarkRepository placeBookmarkRepository;

    @InjectMocks
    private PlaceService placeService;

    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void 위도만_주고_경도와_반경이_없으면_INVALID_SEARCH_CONDITION() {
        assertThatThrownBy(() -> placeService.searchPlaces(null, null, null, null, 37.0, null, null, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SEARCH_CONDITION);

        verifyNoInteractions(placeRepository);
    }

    @Test
    void 반경만_주고_위도_경도가_없으면_INVALID_SEARCH_CONDITION() {
        assertThatThrownBy(() -> placeService.searchPlaces(null, null, null, null, null, null, 1000.0, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SEARCH_CONDITION);
    }

    @Test
    void 검색_조건이_아무것도_없으면_INVALID_SEARCH_CONDITION() {
        assertThatThrownBy(() -> placeService.searchPlaces(null, null, null, null, null, null, null, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SEARCH_CONDITION);

        verifyNoInteractions(placeRepository);
    }
}
