package com.tails.place;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;


//  * Place(장소) 데이터 접근 인터페이스. 기본 CRUD 외에 필요한 조회 메서드만 추가로 선언

public interface PlaceRepository extends JpaRepository<Place, Long> {

    // externalPlaceId(TourAPI contentid) 기준 조회 — 동기화 시 중복 저장 방지
    Optional<Place> findByExternalPlaceId(String externalPlaceId);

    // cat1(대분류 카테고리) 기준 조회
    List<Place> findByCat1(String cat1);

    // cat1 + cat2(대/중분류 카테고리) 기준 조회
    List<Place> findByCat1AndCat2(String cat1, String cat2);

    // placeName에 keyword가 포함된 장소 조회 (LIKE 검색)
    List<Place> findByPlaceNameContaining(String keyword);
}
