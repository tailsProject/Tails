package com.tails.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 장소 리뷰 데이터 관리 JPA Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

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

    // 내가 작성한 리뷰 목록. left join fetch로 장소를 함께 가져와 N+1 방지
    @Query(value = "select r from Review r left join fetch r.place where r.member.id = :memberId order by r.createdAt desc",
            countQuery = "select count(r) from Review r where r.member.id = :memberId")
    Page<Review> findByMemberIdWithPlace(@Param("memberId") Long memberId, Pageable pageable);
}
