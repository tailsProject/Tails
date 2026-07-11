package com.tails.bookmark;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 회원과 장소의 찜 조회 및 관리 Repository
public interface PlaceBookmarkRepository extends JpaRepository<PlaceBookmark, Long> {

    // 찜 여부 확인 및 취소 처리를 위해 찜 엔티티 조회
    Optional<PlaceBookmark> findByPlace_PlaceIdAndMemberId(Long placeId, Long memberId);
}
