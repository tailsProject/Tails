package com.tails.review;

import org.springframework.data.jpa.repository.JpaRepository;

// 장소 리뷰 데이터 관리 JPA Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 이 회원이 이 장소에 이미 리뷰를 작성했는지 확인 (1인 1장소 1리뷰 중복 체크용)
    boolean existsByPlace_PlaceIdAndMember_Id(Long placeId, Long memberId);
}
