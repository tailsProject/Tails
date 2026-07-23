package com.tails.traveldetail;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.Member;
import com.tails.place.Place;
import com.tails.place.PlaceRepository;
import com.tails.travel.Travel;
import com.tails.travel.TravelRepository;
import com.tails.traveldetail.dto.OptimizedRouteResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

// TravelDetailService.suggestOptimizedRoute() 경로 추천 회귀테스트
@ExtendWith(MockitoExtension.class)
class TravelDetailServiceRouteTest {

    @Mock
    private TravelRepository travelRepository;
    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private TravelDetailRepository travelDetailRepository;

    @InjectMocks
    private TravelDetailService travelDetailService;

    private Member newMember(Long id) {
        Member member = Member.builder().email(id + "@test.com").password("x").nickname("n" + id).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Travel newTravel(Long travelId, Member owner) {
        Travel travel = Travel.builder().member(owner).title("t")
                .startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 5)).build();
        ReflectionTestUtils.setField(travel, "travelId", travelId);
        return travel;
    }

    private Place newPlace(Long placeId, Double lat, Double lng) {
        Place place = Place.builder().externalPlaceId("ext" + placeId).placeName("place" + placeId)
                .latitude(lat).longitude(lng).build();
        ReflectionTestUtils.setField(place, "placeId", placeId);
        return place;
    }

    private TravelDetail newDetail(Long detailId, Travel travel, Place place, int sequence) {
        TravelDetail detail = TravelDetail.builder().travel(travel).place(place)
                .travelDate(LocalDate.of(2026, 8, 2)).sequence(sequence).build();
        ReflectionTestUtils.setField(detail, "detailId", detailId);
        return detail;
    }

    @Test
    void 좌표없는_장소가_포함되면_PLACE_LOCATION_MISSING() {
        Travel travel = newTravel(10L, newMember(1L));
        Place noLocation = newPlace(1L, null, null);
        when(travelRepository.findById(10L)).thenReturn(Optional.of(travel));
        when(travelDetailRepository.findByTravel_TravelIdAndTravelDateOrderBySequenceAsc(10L, LocalDate.of(2026, 8, 2)))
                .thenReturn(List.of(newDetail(1L, travel, noLocation, 1)));

        assertThatThrownBy(() -> travelDetailService.suggestOptimizedRoute(10L, 1L, LocalDate.of(2026, 8, 2)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PLACE_LOCATION_MISSING);
    }

    @Test
    void 장소가_1개면_그대로_반환하고_거리는_0이다() {
        Travel travel = newTravel(10L, newMember(1L));
        Place place = newPlace(1L, 37.0, 127.0);
        when(travelRepository.findById(10L)).thenReturn(Optional.of(travel));
        when(travelDetailRepository.findByTravel_TravelIdAndTravelDateOrderBySequenceAsc(10L, LocalDate.of(2026, 8, 2)))
                .thenReturn(List.of(newDetail(1L, travel, place, 1)));

        OptimizedRouteResponse response = travelDetailService.suggestOptimizedRoute(10L, 1L, LocalDate.of(2026, 8, 2));

        assertThat(response.orderedDetails()).hasSize(1);
        assertThat(response.totalDistanceMeters()).isEqualTo(0.0);
    }

    @Test
    void 최근접_이웃_순서로_경로가_정렬된다() {
        Travel travel = newTravel(10L, newMember(1L));
        Place start = newPlace(1L, 37.0, 127.0);
        Place far = newPlace(2L, 37.0, 127.1);
        Place near = newPlace(3L, 37.0, 127.001);
        when(travelRepository.findById(10L)).thenReturn(Optional.of(travel));
        // 리포지토리가 sequence 순으로 [start, far, near]를 반환해도, 출발점(start) 이후엔 항상 가장 가까운 곳부터 방문해야 함
        when(travelDetailRepository.findByTravel_TravelIdAndTravelDateOrderBySequenceAsc(10L, LocalDate.of(2026, 8, 2)))
                .thenReturn(List.of(
                        newDetail(1L, travel, start, 1),
                        newDetail(2L, travel, far, 2),
                        newDetail(3L, travel, near, 3)));

        OptimizedRouteResponse response = travelDetailService.suggestOptimizedRoute(10L, 1L, LocalDate.of(2026, 8, 2));

        assertThat(response.orderedDetails()).extracting("placeId").containsExactly(1L, 3L, 2L);
    }
}
