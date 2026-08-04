package com.tails.travel;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.MemberRepository;
import com.tails.pet.Pet;
import com.tails.pet.PetRepository;
import com.tails.travel.dto.ShareTokenResponse;
import com.tails.travel.dto.SharedTravelResponse;
import com.tails.travel.dto.TravelCreateRequest;
import com.tails.travel.dto.TravelResponse;
import com.tails.travel.dto.TravelUpdateRequest;
import com.tails.traveldetail.TravelDetail;
import com.tails.traveldetail.TravelDetailRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 여행 일정 비즈니스 로직
@Service
@RequiredArgsConstructor
public class TravelService {

    private final TravelRepository travelRepository;
    private final MemberRepository memberRepository;
    private final TravelDetailRepository travelDetailRepository;
    private final PetRepository petRepository;

    // 여행 일정 생성
    @Transactional
    public TravelResponse createTravel(Long memberId, TravelCreateRequest request) {
        validateDateRange(request.startDate(), request.endDate());

        // 조회 없이 프록시만 가져옴, FK로만 사용해 다른 필드는 불필요
        Travel travel = Travel.builder()
                .member(memberRepository.getReferenceById(memberId))
                .title(request.title())
                .description(request.description())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();
        travel.updatePets(resolveOwnedPets(memberId, request.petIds()));

        Travel savedTravel = travelRepository.save(travel);
        return TravelResponse.from(savedTravel, null);
    }

    // 내 여행 일정 목록 페이지 단위 조회
    @Transactional(readOnly = true)
    public Page<TravelResponse> getMyTravels(Long memberId, Pageable pageable) {
        return travelRepository.findByMember_Id(memberId, pageable)
                .map(travel -> TravelResponse.from(travel, findThumbnailUrl(travel.getTravelId())));
    }

    // 여행 일정 상세 조회. 미존재 시 404, 소유자 아니면 403
    @Transactional(readOnly = true)
    public TravelResponse getTravelDetail(Long travelId, Long memberId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_NOT_FOUND));

        if (!travelRepository.existsByTravelIdAndMember_Id(travelId, memberId)) {
            throw new CustomException(ErrorCode.NOT_TRAVEL_OWNER);
        }

        return TravelResponse.from(travel, findThumbnailUrl(travelId));
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

        travel.updateInfo(request.title(), request.description(), request.startDate(), request.endDate());
        travel.updatePets(resolveOwnedPets(memberId, request.petIds()));
        // flush 없으면 커밋 전이라 응답의 updatedAt이 예전 값으로 찍힘
        travelRepository.flush();

        return TravelResponse.from(travel, findThumbnailUrl(travelId));
    }

    // 요청받은 petIds 중 실제 내 반려동물만 필터링, 다른 회원 pet id 도용 방지
    private List<Pet> resolveOwnedPets(Long memberId, List<Long> petIds) {
        if (petIds == null || petIds.isEmpty()) {
            return Collections.emptyList();
        }
        return petRepository.findAllById(petIds).stream()
                .filter(pet -> pet.getMember().getId().equals(memberId))
                .toList();
    }

    // 1일차 첫 방문지의 장소 이미지, 방문지가 없으면 null 반환
    private String findThumbnailUrl(Long travelId) {
        List<TravelDetail> first = travelDetailRepository.findFirstDetail(travelId, PageRequest.of(0, 1));
        if (first.isEmpty() || first.get(0).getPlace() == null) {
            return null;
        }
        return first.get(0).getPlace().getImageUrl();
    }

    // 여행 일정 삭제
    @Transactional
    public void deleteTravel(Long travelId, Long memberId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_NOT_FOUND));

        if (!travelRepository.existsByTravelIdAndMember_Id(travelId, memberId)) {
            throw new CustomException(ErrorCode.NOT_TRAVEL_OWNER);
        }

        travelRepository.delete(travel);
    }

    // 여행 일정 공유 링크 발급, 이미 공유 중이면 기존 링크 그대로 반환
    @Transactional
    public ShareTokenResponse shareTravel(Long travelId, Long memberId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_NOT_FOUND));

        if (!travelRepository.existsByTravelIdAndMember_Id(travelId, memberId)) {
            throw new CustomException(ErrorCode.NOT_TRAVEL_OWNER);
        }

        return new ShareTokenResponse(travel.generateShareToken());
    }

    // 여행 일정 공유 중단, 비공개로 전환
    @Transactional
    public void unshareTravel(Long travelId, Long memberId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_NOT_FOUND));

        if (!travelRepository.existsByTravelIdAndMember_Id(travelId, memberId)) {
            throw new CustomException(ErrorCode.NOT_TRAVEL_OWNER);
        }

        travel.revokeShareToken();
    }

    // 공유 토큰으로 여행 일정 읽기 전용 조회, 토큰 소지 자체가 접근 권한이라 소유권 확인 없음
    @Transactional(readOnly = true)
    public SharedTravelResponse getSharedTravel(String shareToken) {
        Travel travel = travelRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new CustomException(ErrorCode.SHARED_TRAVEL_NOT_FOUND));

        List<TravelDetail> details = travelDetailRepository
                .findByTravel_TravelIdOrderByTravelDateAscSequenceAsc(travel.getTravelId());

        return SharedTravelResponse.of(travel, details, findThumbnailUrl(travel.getTravelId()));
    }

    // 종료일이 시작일보다 빠르면 예외, 생성과 수정 공통 검증
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
}
