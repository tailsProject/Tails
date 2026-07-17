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

    // 찜 많은 순 장소 랭킹용. 장소별로 묶어(group by) 찜 개수 내림차순 정렬하고,
    // 개수가 같으면 placeId 오름차순(2차 정렬)으로 페이지가 바뀌어도 순서가 흔들리지 않게 함.
    // 찜이 하나도 없는 장소는 묶일 그룹 자체가 없어 결과에서 자연히 제외됨(의도된 동작).
    // 각 행이 [Place, 찜 개수(Long)] 쌍의 Object[]로 내려옴 (리뷰 평점 랭킹과 동일한 집계 패턴)
    @Query(value = "select p, count(pb) from PlaceBookmark pb join pb.place p "
            + "group by p order by count(pb) desc, p.placeId asc",
            countQuery = "select count(distinct pb.place) from PlaceBookmark pb")
    Page<Object[]> findPlaceBookmarkCounts(Pageable pageable);
}
