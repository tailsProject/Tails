package com.tails.place;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.place.dto.PlaceResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


// Place(장소) 비즈니스 로직. 조회 전용이라 클래스 레벨에 readOnly 트랜잭션

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;

    
    // 장소 상세 조회.
    // @throws CustomException {@link ErrorCode#PLACE_NOT_FOUND} 해당 placeId의 장소가 없을 때
     
    public PlaceResponse getPlaceDetail(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_FOUND));
        return PlaceResponse.from(place);
    }

    // 장소 목록 페이징 조회
    public Page<PlaceResponse> getPlaces(Pageable pageable) {
        return placeRepository.findAll(pageable)
                .map(PlaceResponse::from);
    }

    // 장소명에 keyword가 포함된 장소 검색. 결과가 없으면 빈 리스트를 반환
    public List<PlaceResponse> searchPlacesByName(String keyword) {
        return placeRepository.findByPlaceNameContaining(keyword).stream()
                .map(PlaceResponse::from)
                .toList();
    }

    // cat1(필수) + cat2(선택) 카테고리로 장소 필터링. cat2가 없으면 cat1만으로 조회
    public List<PlaceResponse> getPlacesByCategory(String cat1, String cat2) {
        List<Place> places = (cat2 == null || cat2.isBlank())
                ? placeRepository.findByCat1(cat1)
                : placeRepository.findByCat1AndCat2(cat1, cat2);

        return places.stream()
                .map(PlaceResponse::from)
                .toList();
    }
}
