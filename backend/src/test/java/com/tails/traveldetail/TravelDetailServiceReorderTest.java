package com.tails.traveldetail;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.Member;
import com.tails.place.Place;
import com.tails.place.PlaceRepository;
import com.tails.travel.Travel;
import com.tails.travel.TravelRepository;
import com.tails.traveldetail.dto.TravelDetailReorderRequest;
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

// TravelDetailService.reorderDetails() 순서 재정렬 검증/재배정 회귀테스트
@ExtendWith(MockitoExtension.class)
class TravelDetailServiceReorderTest {

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

    private Place newPlace(Long placeId) {
        Place place = Place.builder().externalPlaceId("ext" + placeId).placeName("place" + placeId).build();
        ReflectionTestUtils.setField(place, "placeId", placeId);
        return place;
    }

    private TravelDetail newDetail(Long detailId, Travel travel, LocalDate date, int sequence) {
        TravelDetail detail = TravelDetail.builder().travel(travel).place(newPlace(detailId))
                .travelDate(date).sequence(sequence).build();
        ReflectionTestUtils.setField(detail, "detailId", detailId);
        return detail;
    }

    @Test
    void 요청한_id_집합이_기존과_다르면_INVALID_SEQUENCE_REQUEST() {
        Travel travel = newTravel(10L, newMember(1L));
        LocalDate date = LocalDate.of(2026, 8, 2);
        List<TravelDetail> existing = List.of(
                newDetail(1L, travel, date, 1), newDetail(2L, travel, date, 2), newDetail(3L, travel, date, 3));
        when(travelRepository.findById(10L)).thenReturn(Optional.of(travel));
        when(travelDetailRepository.findByTravel_TravelIdAndTravelDateOrderBySequenceAsc(10L, date)).thenReturn(existing);

        TravelDetailReorderRequest request = new TravelDetailReorderRequest(date, List.of(1L, 2L));

        assertThatThrownBy(() -> travelDetailService.reorderDetails(10L, 1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SEQUENCE_REQUEST);
    }

    @Test
    void 정상_요청이면_요청한_순서대로_sequence가_재배정된다() {
        Travel travel = newTravel(10L, newMember(1L));
        LocalDate date = LocalDate.of(2026, 8, 2);
        TravelDetail detail1 = newDetail(1L, travel, date, 1);
        TravelDetail detail2 = newDetail(2L, travel, date, 2);
        TravelDetail detail3 = newDetail(3L, travel, date, 3);
        when(travelRepository.findById(10L)).thenReturn(Optional.of(travel));
        when(travelDetailRepository.findByTravel_TravelIdAndTravelDateOrderBySequenceAsc(10L, date))
                .thenReturn(List.of(detail1, detail2, detail3));

        TravelDetailReorderRequest request = new TravelDetailReorderRequest(date, List.of(3L, 1L, 2L));
        travelDetailService.reorderDetails(10L, 1L, request);

        assertThat(detail3.getSequence()).isEqualTo(1);
        assertThat(detail1.getSequence()).isEqualTo(2);
        assertThat(detail2.getSequence()).isEqualTo(3);
    }
}
