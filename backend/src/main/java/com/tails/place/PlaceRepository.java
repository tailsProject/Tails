package com.tails.place;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


//  * Place(장소) 데이터 접근 인터페이스. 기본 CRUD 외에 필요한 조회 메서드만 추가로 선언

public interface PlaceRepository extends JpaRepository<Place, Long> {

    // externalPlaceId(TourAPI contentid) 기준 조회 — 동기화 시 중복 저장 방지
    Optional<Place> findByExternalPlaceId(String externalPlaceId);

    // cat1(대분류 카테고리) 기준 조회
    List<Place> findByCat1(String cat1);

    // cat1 + cat2(대/중분류 카테고리) 기준 조회
    List<Place> findByCat1AndCat2(String cat1, String cat2);

    // 키워드/카테고리/지역/좌표 범위를 자유롭게 조합해서 검색 (검색 고도화)
    // 전부 선택 조건이라 조합마다 메서드를 만드는 대신 "(:param is null or 조건)" 패턴의 JPQL로 통합
    // 좌표 조건(minLat~maxLng)은 경계 상자(bounding box) 1차 필터 — 정확한 반경 판정은 Service에서 Haversine으로 처리
    @Query("""
            select p from Place p
            where (:keyword is null or p.placeName like concat('%', :keyword, '%') or p.address like concat('%', :keyword, '%'))
              and (:cat1 is null or p.cat1 = :cat1)
              and (:cat2 is null or p.cat2 = :cat2)
              and (:region is null or p.address like concat('%', :region, '%'))
              and (:minLat is null or p.latitude between :minLat and :maxLat)
              and (:minLng is null or p.longitude between :minLng and :maxLng)
            """)
    List<Place> searchPlaces(@Param("keyword") String keyword,
                              @Param("cat1") String cat1,
                              @Param("cat2") String cat2,
                              @Param("region") String region,
                              @Param("minLat") Double minLat,
                              @Param("maxLat") Double maxLat,
                              @Param("minLng") Double minLng,
                              @Param("maxLng") Double maxLng);

    // 검색창 자동완성 — 이름에 keyword가 포함된 장소를 이름순으로 최대 8건
    List<Place> findTop8ByPlaceNameContainingOrderByPlaceNameAsc(String keyword);
}
