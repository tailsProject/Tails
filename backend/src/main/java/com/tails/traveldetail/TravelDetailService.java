package com.tails.traveldetail;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.util.GeoUtil;
import com.tails.place.Place;
import com.tails.place.PlaceRepository;
import com.tails.travel.Travel;
import com.tails.travel.TravelRepository;
import com.tails.traveldetail.dto.OptimizedRouteResponse;
import com.tails.traveldetail.dto.TravelDetailCreateRequest;
import com.tails.traveldetail.dto.TravelDetailReorderRequest;
import com.tails.traveldetail.dto.TravelDetailResponse;
import com.tails.traveldetail.dto.TravelDetailUpdateRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // 하루 일정의 방문 순서를 통째로 재정렬. detailIds는 그 날짜의 세부 일정 전체와 정확히 일치해야 함
    // (travel_id, travel_date, sequence) 유니크 제약 때문에 2단계로 나눠서 처리:
    // 1) 전부 겹치지 않는 음수 임시값으로 옮기고 flush → 2) 요청받은 순서대로 최종값(1부터) 부여
    // (한 번에 바꾸면 예: A 1→2, B 2→1 순서로 바뀌다가 중간에 둘 다 2가 되는 순간 유니크 제약 충돌)
    @Transactional
    public List<TravelDetailResponse> reorderDetails(Long travelId, Long memberId, TravelDetailReorderRequest request) {
        validateTravelOwnership(travelId, memberId);

        List<TravelDetail> existing = travelDetailRepository
                .findByTravel_TravelIdAndTravelDateOrderBySequenceAsc(travelId, request.travelDate());

        List<Long> requestedIds = request.detailIds();
        Set<Long> existingIds = new HashSet<>();
        Map<Long, TravelDetail> byId = new HashMap<>();
        for (TravelDetail detail : existing) {
            existingIds.add(detail.getDetailId());
            byId.put(detail.getDetailId(), detail);
        }
        if (requestedIds.size() != existingIds.size() || !existingIds.equals(new HashSet<>(requestedIds))) {
            throw new CustomException(ErrorCode.INVALID_SEQUENCE_REQUEST);
        }

        int temp = -1;
        for (TravelDetail detail : existing) {
            detail.changeSequence(temp--);
        }
        travelDetailRepository.flush();

        List<TravelDetail> reordered = new ArrayList<>();
        int sequence = 1;
        for (Long detailId : requestedIds) {
            TravelDetail detail = byId.get(detailId);
            detail.changeSequence(sequence++);
            reordered.add(detail);
        }
        travelDetailRepository.flush();

        return reordered.stream()
                .map(TravelDetailResponse::from)
                .toList();
    }

    // 하루 일정의 추천 방문 순서 계산 (경로 최적화). DB에는 반영하지 않고 "추천안"만 계산 —
    // 마음에 들면 이 결과 순서 그대로 reorderDetails를 다시 호출해서 반영하는 2단계 구조
    // 최근접 이웃(nearest-neighbor) 알고리즘: 기존 첫 번째 장소를 출발점으로 고정하고, 이후엔
    // 매번 "지금 위치에서 가장 가까운, 아직 안 간 곳"을 고름 — 완전 최적(TSP)은 아니지만 계산이
    // 빠르고 하루 일정 장소 수 규모에서는 실제 최적해와 큰 차이가 없음
    @Transactional(readOnly = true)
    public OptimizedRouteResponse suggestOptimizedRoute(Long travelId, Long memberId, LocalDate travelDate) {
        validateTravelOwnership(travelId, memberId);

        List<TravelDetail> details = travelDetailRepository
                .findByTravel_TravelIdAndTravelDateOrderBySequenceAsc(travelId, travelDate);

        for (TravelDetail detail : details) {
            Place place = detail.getPlace();
            if (place.getLatitude() == null || place.getLongitude() == null) {
                throw new CustomException(ErrorCode.PLACE_LOCATION_MISSING);
            }
        }

        if (details.size() <= 1) {
            return new OptimizedRouteResponse(
                    details.stream().map(TravelDetailResponse::from).toList(), 0.0);
        }

        List<TravelDetail> remaining = new ArrayList<>(details);
        List<TravelDetail> route = new ArrayList<>();
        double totalDistance = 0.0;

        TravelDetail current = remaining.remove(0);
        route.add(current);

        while (!remaining.isEmpty()) {
            TravelDetail nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (TravelDetail candidate : remaining) {
                double distance = GeoUtil.distanceMeters(
                        current.getPlace().getLatitude(), current.getPlace().getLongitude(),
                        candidate.getPlace().getLatitude(), candidate.getPlace().getLongitude());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = candidate;
                }
            }
            route.add(nearest);
            totalDistance += nearestDistance;
            remaining.remove(nearest);
            current = nearest;
        }

        List<TravelDetailResponse> orderedDetails = route.stream()
                .map(TravelDetailResponse::from)
                .toList();
        return new OptimizedRouteResponse(orderedDetails, totalDistance);
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
