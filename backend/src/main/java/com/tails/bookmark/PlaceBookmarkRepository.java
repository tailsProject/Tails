package com.tails.bookmark;

import com.tails.place.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// 회원과 장소의 찜 조회 및 관리 Repository
public interface PlaceBookmarkRepository extends JpaRepository<PlaceBookmark, Long> {

    // 찜 여부 확인 및 취소 처리를 위해 찜 엔티티 조회
    Optional<PlaceBookmark> findByPlace_PlaceIdAndMemberId(Long placeId, Long memberId);

    // 내가 찜한 장소 목록 (최근 찜 순). 찜이 아니라 장소를 반환해야 해서 join 후 Place를 select
    // PlaceResponse는 Place 자신의 필드만 읽어서 board 쪽과 달리 fetch join은 불필요
    @Query("select p from PlaceBookmark pb join pb.place p where pb.member.id = :memberId order by pb.createdAt desc")
    Page<Place> findBookmarkedPlacesByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    // 마이페이지 통계용 - 내가 찜한 장소 개수
    long countByMemberId(Long memberId);
}
