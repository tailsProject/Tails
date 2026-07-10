package com.tails.traveldetail;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.place.Place;
import com.tails.place.PlaceRepository;
import com.tails.travel.Travel;
import com.tails.travel.TravelRepository;
import com.tails.traveldetail.dto.TravelDetailCreateRequest;
import com.tails.traveldetail.dto.TravelDetailResponse;
import com.tails.traveldetail.dto.TravelDetailUpdateRequest;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// TravelDetail 비즈니스 로직
@Service
@RequiredArgsConstructor
public class TravelDetailService {

    private final TravelRepository travelRepository;
    private final PlaceRepository placeRepository;
    private final TravelDetailRepository travelDetailRepository;

    // 여행 일정에 방문 장소 추가. sequence는 "그 날짜의 마지막 순서 + 1"로 서버가 계산
    @Transactional
    public TravelDetailResponse addTravelDetail(Long travelId, Long memberId, TravelDetailCreateRequest request) {
        Travel travel = validateTravelOwnership(travelId, memberId);

        Place place = placeRepository.findById(request.placeId())
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_FOUND));

        int nextSequence = travelDetailRepository.findMaxSequenceByTravelIdAndDate(travelId, request.travelDate()) + 1;

        TravelDetail travelDetail = TravelDetail.builder()
                .travel(travel)
                .place(place)
                .travelDate(request.travelDate())
                .visitTime(request.visitTime())
                .memo(request.memo())
                .sequence(nextSequence)
                .build();

        TravelDetail savedTravelDetail = travelDetailRepository.save(travelDetail);
        return TravelDetailResponse.from(savedTravelDetail);
    }

    // 전체 세부 일정을 날짜순 → 순서순으로 조회
    @Transactional(readOnly = true)
    public List<TravelDetailResponse> getTravelDetails(Long travelId, Long memberId) {
        validateTravelOwnership(travelId, memberId);

        return travelDetailRepository.findByTravel_TravelIdOrderByTravelDateAscSequenceAsc(travelId).stream()
                .map(TravelDetailResponse::from)
                .toList();
    }

    // 특정 하루치 세부 일정만 순서대로 조회
    @Transactional(readOnly = true)
    public List<TravelDetailResponse> getTravelDetailsByDate(Long travelId, Long memberId, LocalDate travelDate) {
        validateTravelOwnership(travelId, memberId);

        return travelDetailRepository.findByTravel_TravelIdAndTravelDateOrderBySequenceAsc(travelId, travelDate).stream()
                .map(TravelDetailResponse::from)
                .toList();
    }

    // 세부 일정의 방문 시간/메모 수정
    @Transactional
    public TravelDetailResponse updateTravelDetail(
            Long travelId, Long detailId, Long memberId, TravelDetailUpdateRequest request) {
        validateTravelOwnership(travelId, memberId);

        TravelDetail travelDetail = travelDetailRepository.findById(detailId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_DETAIL_NOT_FOUND));

        // detailId가 다른 여행 일정 소속이면 차단
        if (!travelDetail.getTravel().getTravelId().equals(travelId)) {
            throw new CustomException(ErrorCode.TRAVEL_DETAIL_NOT_FOUND);
        }

        travelDetail.updateInfo(request.visitTime(), request.memo());
        // flush 없으면 응답의 updatedAt이 예전 값으로 찍힘 (커밋 전이라 @PreUpdate 미실행)
        travelDetailRepository.flush();

        return TravelDetailResponse.from(travelDetail);
    }

    // 세부 일정 삭제
    @Transactional
    public void deleteTravelDetail(Long travelId, Long detailId, Long memberId) {
        validateTravelOwnership(travelId, memberId);

        TravelDetail travelDetail = travelDetailRepository.findById(detailId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_DETAIL_NOT_FOUND));

        // detailId가 다른 여행 일정 소속이면 차단
        if (!travelDetail.getTravel().getTravelId().equals(travelId)) {
            throw new CustomException(ErrorCode.TRAVEL_DETAIL_NOT_FOUND);
        }

        travelDetailRepository.delete(travelDetail);
    }

    // travelId 존재 + 소유권 확인 공통 로직. 이후 로직에서 Travel 엔티티가 필요해 findById로 조회
    private Travel validateTravelOwnership(Long travelId, Long memberId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_NOT_FOUND));

        if (!travel.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_TRAVEL_OWNER);
        }

        return travel;
    }
}
