package com.tails.traveldetail;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 여행 세부 일정 조회 레포지토리
public interface TravelDetailRepository extends JpaRepository<TravelDetail, Long> {

    // 목록 응답이 건마다 place 필드를 읽으므로 fetch join으로 N+1 방지
    @Query(value = "select td from TravelDetail td left join fetch td.place where td.travel.travelId = :travelId "
            + "order by td.travelDate asc, td.sequence asc")
    List<TravelDetail> findByTravel_TravelIdOrderByTravelDateAscSequenceAsc(@Param("travelId") Long travelId);

    // 여행 카드 썸네일용, 화면에 실제로 첫 방문지로 보이는 시간순 정렬 기준과 동일하게 맞춤
    @Query(value = "select td from TravelDetail td left join fetch td.place where td.travel.travelId = :travelId "
            + "order by td.travelDate asc, case when td.visitTime is null then 1 else 0 end asc, "
            + "td.visitTime asc, td.sequence asc")
    List<TravelDetail> findFirstDetail(@Param("travelId") Long travelId, Pageable pageable);

    @Query(value = "select td from TravelDetail td left join fetch td.place "
            + "where td.travel.travelId = :travelId and td.travelDate = :travelDate order by td.sequence asc")
    List<TravelDetail> findByTravel_TravelIdAndTravelDateOrderBySequenceAsc(
            @Param("travelId") Long travelId, @Param("travelDate") LocalDate travelDate);

    // 새 장소 추가 시 마지막 순서 다음 값으로 세팅, 값이 없으면 0
    @Query("SELECT COALESCE(MAX(td.sequence), 0) FROM TravelDetail td "
            + "WHERE td.travel.travelId = :travelId AND td.travelDate = :travelDate")
    Integer findMaxSequenceByTravelIdAndDate(@Param("travelId") Long travelId, @Param("travelDate") LocalDate travelDate);
}
