package com.tails.travel;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import com.tails.travel.dto.ShareTokenResponse;
import com.tails.travel.dto.TravelCreateRequest;
import com.tails.travel.dto.TravelUpdateRequest;
import com.tails.traveldetail.TravelDetailRepository;
import java.time.LocalDate;
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

// TravelService 단위테스트 - 날짜범위 검증/소유권 확인/공유링크 재발급 및 중단
@ExtendWith(MockitoExtension.class)
class TravelServiceTest {

    @Mock
    private TravelRepository travelRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private TravelDetailRepository travelDetailRepository;

    @InjectMocks
    private TravelService travelService;

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
    void 생성_시_종료일이_시작일보다_빠르면_INVALID_DATE_RANGE() {
        TravelCreateRequest request = new TravelCreateRequest("t", LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> travelService.createTravel(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    void 수정_시_종료일이_시작일보다_빠르면_INVALID_DATE_RANGE() {
        TravelUpdateRequest request = new TravelUpdateRequest("t", LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> travelService.updateTravel(10L, 1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    void 소유자가_아니면_조회할_수_없다_NOT_TRAVEL_OWNER() {
        Travel travel = newTravel(10L, newMember(1L), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
        when(travelRepository.findById(10L)).thenReturn(Optional.of(travel));
        when(travelRepository.existsByTravelIdAndMember_Id(10L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> travelService.getTravelDetail(10L, 99L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_TRAVEL_OWNER);
    }

    @Test
    void 소유자가_아니면_삭제할_수_없다_NOT_TRAVEL_OWNER() {
        Travel travel = newTravel(10L, newMember(1L), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
        when(travelRepository.findById(10L)).thenReturn(Optional.of(travel));
        when(travelRepository.existsByTravelIdAndMember_Id(10L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> travelService.deleteTravel(10L, 99L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_TRAVEL_OWNER);
    }

    @Test
    void 공유링크를_재발급하면_새_토큰으로_교체되어_이전_토큰과_달라진다() {
        Travel travel = newTravel(10L, newMember(1L), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
        when(travelRepository.findById(10L)).thenReturn(Optional.of(travel));
        when(travelRepository.existsByTravelIdAndMember_Id(10L, 1L)).thenReturn(true);

        ShareTokenResponse first = travelService.shareTravel(10L, 1L);
        ShareTokenResponse second = travelService.shareTravel(10L, 1L);

        assertThat(first.shareToken()).isNotEqualTo(second.shareToken());
    }

    @Test
    void 공유_중단_후_그_토큰으로_조회하면_SHARED_TRAVEL_NOT_FOUND() {
        when(travelRepository.findByShareToken("revoked-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> travelService.getSharedTravel("revoked-token"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SHARED_TRAVEL_NOT_FOUND);
    }
}
