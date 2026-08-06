package com.tails.traveldetail;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.Member;
import com.tails.place.PlaceRepository;
import com.tails.traveldetail.dto.TravelDetailCreateRequest;
import com.tails.travel.Travel;
import com.tails.travel.TravelRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// TravelDetailService.addTravelDetail() 날짜범위(여행 기간 내) 검증 회귀테스트
@ExtendWith(MockitoExtension.class)
class TravelDetailServiceDateRangeTest {

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

    private Travel newTravel(Long travelId, Member owner, LocalDate start, LocalDate end) {
        Travel travel = Travel.builder().member(owner).title("t").startDate(start).endDate(end).build();
        ReflectionTestUtils.setField(travel, "travelId", travelId);
        return travel;
    }

    @Test
    void 여행_시작일보다_이른_날짜로_세부일정을_추가하면_INVALID_DATE_RANGE() {
        Member owner = newMember(1L);
        Travel travel = newTravel(10L, owner, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
        when(travelRepository.findById(10L)).thenReturn(Optional.of(travel));

        TravelDetailCreateRequest request = new TravelDetailCreateRequest(99L, LocalDate.of(2026, 7, 31), null, null);

        assertThatThrownBy(() -> travelDetailService.addTravelDetail(10L, 1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_DATE_RANGE);

        verifyNoInteractions(placeRepository);
    }

    @Test
    void 여행_종료일보다_늦은_날짜로_세부일정을_추가하면_INVALID_DATE_RANGE() {
        Member owner = newMember(1L);
        Travel travel = newTravel(10L, owner, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
        when(travelRepository.findById(10L)).thenReturn(Optional.of(travel));

        TravelDetailCreateRequest request = new TravelDetailCreateRequest(99L, LocalDate.of(2026, 8, 4), null, null);

        assertThatThrownBy(() -> travelDetailService.addTravelDetail(10L, 1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_DATE_RANGE);

        verifyNoInteractions(placeRepository);
    }
}
