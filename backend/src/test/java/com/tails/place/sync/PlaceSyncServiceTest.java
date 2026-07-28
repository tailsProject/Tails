package com.tails.place.sync;

import com.tails.place.Place;
import com.tails.place.PlaceRepository;
import com.tails.place.sync.dto.CategoryCountSummaryResponse;
import com.tails.place.sync.dto.PetTourListItem;
import com.tails.place.sync.dto.SyncRunResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 이슈 #13: 동기화 일일 처리량 실측치 반영, 지역코드 필터, 쇼핑 하위분류 제외, 좌표 오류 데이터 정제
class PlaceSyncServiceTest {

    private PetTourApiClient petTourApiClient = mock(PetTourApiClient.class);
    private PlaceRepository placeRepository = mock(PlaceRepository.class);
    private PlaceSyncService service = new PlaceSyncService(petTourApiClient, placeRepository);

    private PetTourListItem listItem(String contentId, String contentTypeId, String lclsSystm2,
                                      String mapx, String mapy) {
        return new PetTourListItem(
                "주소1", "주소2", contentId, contentTypeId, "20260101",
                "img.jpg", null, mapx, mapy, "20260101", "02-000-0000", "테스트 장소",
                "cat1", lclsSystm2, "cat3");
    }

    @Test
    void 예상_소요일수는_하루_1000건_기준으로_계산된다() {
        when(petTourApiClient.fetchTotalCount(anyString())).thenReturn(0L);
        // 관광지 카테고리만 2500건이라고 가정 (다른 6개 카테고리는 0)
        when(petTourApiClient.fetchTotalCount(eq(PetTourContentType.TOURIST_SPOT.getCode()))).thenReturn(2500L);

        CategoryCountSummaryResponse response = service.getCategoryCounts();

        // 900건 기준이면 올림 3일(2500/900=2.77→3)이 아니라, 1000건 기준으로 올림 3일(2500/1000=2.5→3)
        // 값 자체는 같은 케이스라 더 명확한 경계값으로 재확인
        assertThat(response.estimatedDays()).isEqualTo(3);
    }

    @Test
    void 예상_소요일수_900건_기준과_1000건_기준이_실제로_다르게_나오는_경계값() {
        when(petTourApiClient.fetchTotalCount(anyString())).thenReturn(0L);
        when(petTourApiClient.fetchTotalCount(eq(PetTourContentType.TOURIST_SPOT.getCode()))).thenReturn(1000L);

        CategoryCountSummaryResponse response = service.getCategoryCounts();

        // 900건 기준이면 1000/900=1.11→2일이 나와야 하지만, 1000건 기준이면 정확히 1일
        assertThat(response.estimatedDays()).isEqualTo(1);
    }

    @Test
    void areaCode가_fetchSyncList에_그대로_전달된다() {
        when(petTourApiClient.fetchSyncList(anyString(), anyString(), anyInt(), anyInt())).thenReturn(List.of());

        service.previewSync(PetTourContentType.TOURIST_SPOT, "32", 1, 5);

        verify(petTourApiClient).fetchSyncList(PetTourContentType.TOURIST_SPOT.getCode(), "32", 1, 5);
    }

    @Test
    void areaCode가_null이어도_그대로_null로_전달된다() {
        when(petTourApiClient.fetchSyncList(anyString(), isNull(), anyInt(), anyInt())).thenReturn(List.of());

        service.previewSync(PetTourContentType.TOURIST_SPOT, null, 1, 5);

        verify(petTourApiClient).fetchSyncList(eq(PetTourContentType.TOURIST_SPOT.getCode()), isNull(), eq(1), eq(5));
    }

    @Test
    void 쇼핑_카테고리의_개별상점_편의점_하위분류는_동기화에서_제외되고_카운트된다() {
        PetTourListItem normalShop = listItem("1", "38", "SH02", "127.0", "37.5"); // 대형 쇼핑몰 - 포함
        PetTourListItem excludedShop1 = listItem("2", "38", "SH04", "127.0", "37.5"); // 개별 상점 - 제외
        PetTourListItem excludedShop2 = listItem("3", "38", "SH07", "127.0", "37.5"); // 편의점 - 제외
        when(petTourApiClient.fetchSyncList(anyString(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(normalShop, excludedShop1, excludedShop2));
        when(placeRepository.findByExternalPlaceId(anyString())).thenReturn(Optional.empty());
        when(petTourApiClient.fetchDetail(anyString())).thenReturn(null);
        when(placeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncRunResponse response = service.syncCategory(PetTourContentType.SHOPPING, null, 1, 100);

        assertThat(response.excludedBySubcategory()).isEqualTo(2);
        assertThat(response.newlySaved()).isEqualTo(1);
        assertThat(response.totalFetched()).isEqualTo(3);
    }

    @Test
    void 쇼핑이_아닌_카테고리는_lclsSystm2가_SH04여도_제외되지_않는다() {
        // SHOPPING_CONTENT_TYPE("38")이 아니면 lclsSystm2 값과 무관하게 제외 로직이 적용되면 안 됨
        PetTourListItem touristSpot = listItem("1", PetTourContentType.TOURIST_SPOT.getCode(), "SH04", "127.0", "37.5");
        when(petTourApiClient.fetchSyncList(anyString(), any(), anyInt(), anyInt())).thenReturn(List.of(touristSpot));
        when(placeRepository.findByExternalPlaceId(anyString())).thenReturn(Optional.empty());
        when(petTourApiClient.fetchDetail(anyString())).thenReturn(null);
        when(placeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncRunResponse response = service.syncCategory(PetTourContentType.TOURIST_SPOT, null, 1, 100);

        assertThat(response.excludedBySubcategory()).isEqualTo(0);
        assertThat(response.newlySaved()).isEqualTo(1);
    }

    @Test
    void 좌표값이_0이면_null로_저장된다() {
        PetTourListItem zeroCoord = listItem("1", PetTourContentType.TOURIST_SPOT.getCode(), "cat2", "0", "0");
        when(petTourApiClient.fetchSyncList(anyString(), any(), anyInt(), anyInt())).thenReturn(List.of(zeroCoord));
        when(placeRepository.findByExternalPlaceId(anyString())).thenReturn(Optional.empty());
        when(petTourApiClient.fetchDetail(anyString())).thenReturn(null);
        when(placeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.syncCategory(PetTourContentType.TOURIST_SPOT, null, 1, 100);

        var captor = org.mockito.ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).save(captor.capture());
        assertThat(captor.getValue().getLatitude()).isNull();
        assertThat(captor.getValue().getLongitude()).isNull();
    }

    @Test
    void 좌표값이_빈문자열이면_기존처럼_null로_저장된다() {
        PetTourListItem blankCoord = listItem("1", PetTourContentType.TOURIST_SPOT.getCode(), "cat2", "", "");
        when(petTourApiClient.fetchSyncList(anyString(), any(), anyInt(), anyInt())).thenReturn(List.of(blankCoord));
        when(placeRepository.findByExternalPlaceId(anyString())).thenReturn(Optional.empty());
        when(petTourApiClient.fetchDetail(anyString())).thenReturn(null);
        when(placeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.syncCategory(PetTourContentType.TOURIST_SPOT, null, 1, 100);

        var captor = org.mockito.ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).save(captor.capture());
        assertThat(captor.getValue().getLatitude()).isNull();
    }

    @Test
    void 정상_좌표값은_그대로_파싱되어_저장된다() {
        PetTourListItem validCoord = listItem("1", PetTourContentType.TOURIST_SPOT.getCode(), "cat2", "127.123", "37.456");
        when(petTourApiClient.fetchSyncList(anyString(), any(), anyInt(), anyInt())).thenReturn(List.of(validCoord));
        when(placeRepository.findByExternalPlaceId(anyString())).thenReturn(Optional.empty());
        when(petTourApiClient.fetchDetail(anyString())).thenReturn(null);
        when(placeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.syncCategory(PetTourContentType.TOURIST_SPOT, null, 1, 100);

        var captor = org.mockito.ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).save(captor.capture());
        assertThat(captor.getValue().getLongitude()).isEqualTo(127.123);
        assertThat(captor.getValue().getLatitude()).isEqualTo(37.456);
    }
}
