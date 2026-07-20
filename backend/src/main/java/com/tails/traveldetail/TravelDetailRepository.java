package com.tails.traveldetail;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TravelDetailRepository extends JpaRepository<TravelDetail, Long> {

    // 여행 일정 전체를 날짜순 → 같은 날짜 안에서는 순서(sequence)순으로 조회
    List<TravelDetail> findByTravel_TravelIdOrderByTravelDateAscSequenceAsc(Long travelId);

    // 특정 하루치만 순서대로 조회
    List<TravelDetail> findByTravel_TravelIdAndTravelDateOrderBySequenceAsc(Long travelId, LocalDate travelDate);

    // 새 장소 추가 시 "현재 마지막 순서 + 1"을 계산하기 위한 최댓값 조회
    // 집계 함수(MAX)가 필요해서 메서드 이름 규칙 대신 JPQL을 직접 씀
    // COALESCE(..., 0): 그 날짜에 등록된 게 하나도 없으면 MAX가 null이라 0으로 대체 (첫 방문이면 0+1=1).
    @Query("SELECT COALESCE(MAX(td.sequence), 0) FROM TravelDetail td "
            + "WHERE td.travel.travelId = :travelId AND td.travelDate = :travelDate")
    Integer findMaxSequenceByTravelIdAndDate(@Param("travelId") Long travelId, @Param("travelDate") LocalDate travelDate);
}
