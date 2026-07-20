package com.tails.travel;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelRepository extends JpaRepository<Travel, Long> {

    // 마이페이지 "내 여행 일정" 목록용. 페이지 단위 조회 (Place/Review/PlaceBookmark 목록과 방식 통일)
    Page<Travel> findByMember_Id(Long memberId, Pageable pageable);

    // 수정/삭제 전 소유권 확인용. 다른 회원의 travelId를 끼워 넣어 접근하는 걸 방지
    boolean existsByTravelIdAndMember_Id(Long travelId, Long memberId);

    // 여행 리마인더 스케줄러용
    List<Travel> findByStartDate(LocalDate startDate);

    // 공유 링크(GET /api/travels/shared/{token})용. shareToken이 unique라 0건 또는 1건만 나옴
    Optional<Travel> findByShareToken(String shareToken);

    // 마이페이지 통계용 - 내 여행 일정 개수
    long countByMember_Id(Long memberId);
}
