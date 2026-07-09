package com.tails.travel;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.MemberRepository;
import com.tails.travel.dto.TravelCreateRequest;
import com.tails.travel.dto.TravelResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Travel 비즈니스 로직
@Service
@RequiredArgsConstructor
public class TravelService {

    private final TravelRepository travelRepository;
    private final MemberRepository memberRepository;

    // 여행 일정 생성
    @Transactional
    public TravelResponse createTravel(Long memberId, TravelCreateRequest request) {
        validateDateRange(request.startDate(), request.endDate());

        // getReferenceById: SELECT 없이 프록시만 가져옴 (FK로만 쓰여서 다른 필드 불필요)
        Travel travel = Travel.builder()
                .member(memberRepository.getReferenceById(memberId))
                .title(request.title())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();

        Travel savedTravel = travelRepository.save(travel);
        return TravelResponse.from(savedTravel);
    }

    // 내 여행 일정 목록 조회
    @Transactional(readOnly = true)
    public List<TravelResponse> getMyTravels(Long memberId) {
        return travelRepository.findByMember_Id(memberId).stream()
                .map(TravelResponse::from)
                .toList();
    }

    // 종료일이 시작일보다 빠르면 예외 (createTravel/updateTravel 공통)
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
}
