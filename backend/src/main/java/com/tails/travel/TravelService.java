package com.tails.travel;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.MemberRepository;
import com.tails.travel.dto.TravelCreateRequest;
import com.tails.travel.dto.TravelResponse;
import com.tails.travel.dto.TravelUpdateRequest;
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

    // 여행 일정 상세 조회. 미존재 시 404, 소유자 아니면 403
    @Transactional(readOnly = true)
    public TravelResponse getTravelDetail(Long travelId, Long memberId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_NOT_FOUND));

        if (!travelRepository.existsByTravelIdAndMember_Id(travelId, memberId)) {
            throw new CustomException(ErrorCode.NOT_TRAVEL_OWNER);
        }

        return TravelResponse.from(travel);
    }

    // 여행 일정 수정
    @Transactional
    public TravelResponse updateTravel(Long travelId, Long memberId, TravelUpdateRequest request) {
        validateDateRange(request.startDate(), request.endDate());

        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_NOT_FOUND));

        if (!travelRepository.existsByTravelIdAndMember_Id(travelId, memberId)) {
            throw new CustomException(ErrorCode.NOT_TRAVEL_OWNER);
        }

        travel.updateInfo(request.title(), request.startDate(), request.endDate());
        // flush 없으면 응답에 updatedAt이 예전 값으로 찍힘 (커밋 전이라 @PreUpdate 미실행)
        travelRepository.flush();

        return TravelResponse.from(travel);
    }

    // 종료일이 시작일보다 빠르면 예외 (createTravel/updateTravel 공통)
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
}
