package com.tails.review;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 장소 리뷰 데이터 관리 JPA Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 메인페이지 "최근 리뷰" 미리보기용 - 전체 장소를 통틀어 최신순 N건
    @Query("select r from Review r left join fetch r.member left join fetch r.place order by r.createdAt desc")
    List<Review> findRecentWithMemberAndPlace(Pageable pageable);

    // 이 회원이 이 장소에 이미 리뷰를 작성했는지 확인 (1인 1장소 1리뷰 중복 체크용)
    boolean existsByPlace_PlaceIdAndMember_Id(Long placeId, Long memberId);

    // 장소 리뷰 목록. left join fetch로 작성자를 함께 가져와 N+1 방지 (BoardRepository.findAllWithMember와 동일한 이유)
    @Query(value = "select r from Review r left join fetch r.member where r.place.placeId = :placeId order by r.createdAt desc",
            countQuery = "select count(r) from Review r where r.place.placeId = :placeId")
    Page<Review> findByPlaceIdWithMember(@Param("placeId") Long placeId, Pageable pageable);

    // 이 장소의 평균 별점. 리뷰가 하나도 없으면 avg 결과가 null이라 Service에서 null 체크 필요
    @Query("select avg(r.rating) from Review r where r.place.placeId = :placeId")
    Double findAverageRatingByPlaceId(@Param("placeId") Long placeId);

    long countByPlace_PlaceId(Long placeId);

    // 여러 장소의 평균 별점/리뷰 수를 한 번에 조회(N+1 방지). 리뷰가 없는 장소는 group by 결과에
    // 아예 안 잡히므로 Service에서 조회되지 않은 placeId는 리뷰 없음으로 처리해야 함
    @Query("select r.place.placeId, avg(r.rating), count(r) from Review r where r.place.placeId in :placeIds group by r.place.placeId")
    List<Object[]> findRatingSummariesByPlaceIds(@Param("placeIds") List<Long> placeIds);

    // 내가 작성한 리뷰 목록. left join fetch로 장소를 함께 가져와 N+1 방지
    @Query(value = "select r from Review r left join fetch r.place where r.member.id = :memberId order by r.createdAt desc",
            countQuery = "select count(r) from Review r where r.member.id = :memberId")
    Page<Review> findByMemberIdWithPlace(@Param("memberId") Long memberId, Pageable pageable);

    // 마이페이지 통계용 - 내가 작성한 리뷰 개수
    long countByMember_Id(Long memberId);

    // 평점 높은 순 장소 랭킹용. 장소별로 묶어(group by) 평균 별점 내림차순 정렬하고,
    // 평점이 같으면 placeId 오름차순(2차 정렬)으로 페이지가 바뀌어도 순서가 흔들리지 않게 함.
    // 리뷰가 하나도 없는 장소는 묶일 그룹 자체가 없어 결과에서 자연히 제외됨(의도된 동작).
    // 반환 타입이 엔티티가 아니라 Object[] — 각 행이 [Place, 평균 별점(Double)] 쌍으로 내려옴
    @Query(value = "select r.place, avg(r.rating) from Review r group by r.place order by avg(r.rating) desc, r.place.placeId asc",
            countQuery = "select count(distinct r.place) from Review r")
    Page<Object[]> findPlacesOrderByAverageRating(Pageable pageable);
}
