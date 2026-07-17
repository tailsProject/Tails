package com.tails.place;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.util.GeoUtil;
import com.tails.place.dto.PlaceRatingResponse;
import com.tails.place.dto.PlaceResponse;
import com.tails.place.dto.PlaceSearchResponse;
import com.tails.review.ReviewRepository;
import java.util.Comparator;
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
    // 랭킹 조회 전용. Review 테이블을 집계한 결과라 이 Service가 직접 참조 —
    // ReviewService를 거치지 않는 이유는 집계 쿼리 하나만 필요할 뿐, 리뷰 CRUD 책임까지 끌어올 필요가 없어서
    private final ReviewRepository reviewRepository;

    
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

    // 통합 검색 — 키워드/카테고리/지역/반경(좌표) 조건을 자유롭게 조합. keyword만 넘기면 기존 장소명 검색과 동일하게 동작
    // 반경 검색은 2단계: 경계 상자로 DB에서 후보를 크게 줄인 뒤(GeoUtil.latDelta/lngDelta),
    // Haversine으로 정확한 거리를 계산해 반경 밖을 제외하고 가까운 순 정렬
    // @throws CustomException {@link ErrorCode#INVALID_SEARCH_CONDITION} 조건이 하나도 없거나, 좌표 3종(lat/lng/radius) 중 일부만 왔거나, radius가 0 이하인 경우
    public List<PlaceSearchResponse> searchPlaces(String keyword, String cat1, String cat2,
            String region, Double lat, Double lng, Double radiusMeters) {
        keyword = blankToNull(keyword);
        cat1 = blankToNull(cat1);
        cat2 = blankToNull(cat2);
        region = blankToNull(region);

        boolean anyGeo = lat != null || lng != null || radiusMeters != null;
        boolean allGeo = lat != null && lng != null && radiusMeters != null;
        if (anyGeo && (!allGeo || radiusMeters <= 0)) {
            throw new CustomException(ErrorCode.INVALID_SEARCH_CONDITION);
        }
        if (!allGeo && keyword == null && cat1 == null && cat2 == null && region == null) {
            throw new CustomException(ErrorCode.INVALID_SEARCH_CONDITION);
        }

        Double minLat = null;
        Double maxLat = null;
        Double minLng = null;
        Double maxLng = null;
        if (allGeo) {
            double latDelta = GeoUtil.latDelta(radiusMeters);
            double lngDelta = GeoUtil.lngDelta(radiusMeters, lat);
            minLat = lat - latDelta;
            maxLat = lat + latDelta;
            minLng = lng - lngDelta;
            maxLng = lng + lngDelta;
        }

        List<Place> candidates = placeRepository.searchPlaces(
                keyword, cat1, cat2, region, minLat, maxLat, minLng, maxLng);

        if (!allGeo) {
            return candidates.stream()
                    .map(PlaceSearchResponse::from)
                    .toList();
        }

        final double centerLat = lat;
        final double centerLng = lng;
        final double radius = radiusMeters;
        return candidates.stream()
                .map(place -> PlaceSearchResponse.of(place,
                        GeoUtil.distanceMeters(centerLat, centerLng, place.getLatitude(), place.getLongitude())))
                .filter(response -> response.getDistanceMeters() <= radius)
                .sorted(Comparator.comparing(PlaceSearchResponse::getDistanceMeters))
                .toList();
    }

    // 빈 문자열/공백만 있는 파라미터를 null("조건 없음")로 통일
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
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

    // 평점 높은 순 장소 랭킹. 집계 쿼리가 [Place, 평균 별점] 쌍의 Object[]로 반환하므로
    // 각 자리 타입을 아는 여기서 캐스팅해 dto로 변환 (정렬은 쿼리에 고정돼 있어 Pageable의 sort는 안 쓰임)
    public Page<PlaceRatingResponse> getPlacesRankedByRating(Pageable pageable) {
        return reviewRepository.findPlacesOrderByAverageRating(pageable)
                .map(row -> PlaceRatingResponse.of((Place) row[0], (Double) row[1]));
    }
}
