package com.tails.traveldetail;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.place.Place;
import com.tails.place.PlaceRepository;
import com.tails.travel.Travel;
import com.tails.travel.TravelRepository;
import com.tails.traveldetail.dto.TravelDetailCreateRequest;
import com.tails.traveldetail.dto.TravelDetailResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// TravelDetail 비즈니스 로직. Travel(소속 일정)과 Place(방문 장소) 두 도메인을 잇는 엔티티라
// TravelRepository/PlaceRepository도 함께 주입받음
@Service
@RequiredArgsConstructor
public class TravelDetailService {

    private final TravelRepository travelRepository;
    private final PlaceRepository placeRepository;
    private final TravelDetailRepository travelDetailRepository;

    // 여행 일정에 방문 장소 추가. sequence는 "그 날짜의 현재 마지막 순서 + 1"로 서버가 계산
    // (클라이언트가 직접 정하게 하면 중복/건너뜀 실수가 생기기 쉬움).
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

    // travelId 존재 + memberId 소유 확인 공통 로직.
    // TravelService와 달리 existsBy가 아니라 findById로 가져오는 이유: 이후 로직에서
    // Travel 엔티티 자체가 필요해서(연관관계로 걸어야 함) 조회 한 번으로 겸함
    private Travel validateTravelOwnership(Long travelId, Long memberId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_NOT_FOUND));

        if (!travel.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_TRAVEL_OWNER);
        }

        return travel;
    }
}
