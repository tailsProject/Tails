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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

// PlaceService.searchPlaces() 좌표 범위(lat -90~90, lng -180~180) 검증 회귀테스트
@ExtendWith(MockitoExtension.class)
class PlaceServiceGeoValidationTest {

    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private PlaceBookmarkRepository placeBookmarkRepository;

    @InjectMocks
    private PlaceService placeService;

    @Test
    void 위도가_90을_초과하면_INVALID_SEARCH_CONDITION() {
        assertThatThrownBy(() -> placeService.searchPlaces(null, null, null, null, 91.0, 127.0, 1000.0))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SEARCH_CONDITION);

        verifyNoInteractions(placeRepository);
    }

    @Test
    void 위도가_영하90_미만이면_INVALID_SEARCH_CONDITION() {
        assertThatThrownBy(() -> placeService.searchPlaces(null, null, null, null, -91.0, 127.0, 1000.0))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SEARCH_CONDITION);
    }

    @Test
    void 경도가_180을_초과하면_INVALID_SEARCH_CONDITION() {
        assertThatThrownBy(() -> placeService.searchPlaces(null, null, null, null, 37.0, 181.0, 1000.0))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SEARCH_CONDITION);
    }

    @Test
    void 경도가_영하180_미만이면_INVALID_SEARCH_CONDITION() {
        assertThatThrownBy(() -> placeService.searchPlaces(null, null, null, null, 37.0, -181.0, 1000.0))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SEARCH_CONDITION);
    }

    @Test
    void radius가_0이하이면_여전히_INVALID_SEARCH_CONDITION() {
        assertThatThrownBy(() -> placeService.searchPlaces(null, null, null, null, 37.0, 127.0, 0.0))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SEARCH_CONDITION);
    }
}
