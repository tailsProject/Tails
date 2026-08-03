package com.tails.place;

import com.tails.bookmark.PlaceBookmarkRepository;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.util.GeoUtil;
import com.tails.place.dto.PlaceAutocompleteResponse;
import com.tails.place.dto.PlaceBookmarkCountResponse;
import com.tails.place.dto.PlaceRatingResponse;
import com.tails.place.dto.PlaceResponse;
import com.tails.place.dto.PlaceSearchResponse;
import com.tails.review.ReviewRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


// Place(장소) 비즈니스 로직. 조회 전용이라 클래스 레벨에 readOnly 트랜잭션

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final PlaceImageRepository placeImageRepository;
    // 인기 랭킹 조회 전용. PlaceBookmark 테이블을 집계한 결과라 이 Service가 직접 참조 —
    // BookmarkService를 거치지 않는 이유는 집계 쿼리 하나만 필요할 뿐, 찜 토글 책임까지 끌어올 필요가 없어서
    private final PlaceBookmarkRepository placeBookmarkRepository;
    // 랭킹 조회 전용. Review 테이블을 집계한 결과라 이 Service가 직접 참조 —
    // ReviewService를 거치지 않는 이유는 집계 쿼리 하나만 필요할 뿐, 리뷰 CRUD 책임까지 끌어올 필요가 없어서
    private final ReviewRepository reviewRepository;

    // "이 지역에서 검색"처럼 지도를 축소한 상태로 반경 검색하면 결과가 수백~수천 건까지 잡힐 수 있어서,
    // 거리순으로 가까운 것부터 이 개수만 반환(카카오맵/네이버맵 등 실제 지도 서비스도 결과 상한을 둠)
    private static final int MAX_GEO_SEARCH_RESULTS = 100;


    // 장소 상세 조회. currentMemberId가 있으면 내 찜 여부도 함께 계산(비로그인이면 항상 false)
    // @throws CustomException {@link ErrorCode#PLACE_NOT_FOUND} 해당 placeId의 장소가 없을 때
    public PlaceResponse getPlaceDetail(Long placeId, Long currentMemberId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_FOUND));
        List<String> imageUrls = placeImageRepository.findByPlace_PlaceIdOrderBySequenceAsc(placeId).stream()
                .map(PlaceImage::getImageUrl)
                .toList();
        boolean bookmarked = currentMemberId != null
                && placeBookmarkRepository.findByPlace_PlaceIdAndMemberId(placeId, currentMemberId).isPresent();
        return PlaceResponse.from(place, imageUrls, bookmarked);
    }

    // 장소 목록 페이징 조회
    public Page<PlaceResponse> getPlaces(Pageable pageable) {
        return placeRepository.findAll(pageable)
                .map(PlaceResponse::from);
    }

    // 통합 검색 — 키워드/카테고리/지역/반경(좌표) 조건을 자유롭게 조합. keyword만 넘기면 기존 장소명 검색과 동일하게 동작
    // 반경 검색은 2단계: 경계 상자로 DB에서 후보를 크게 줄인 뒤(GeoUtil.latDelta/lngDelta),
    // Haversine으로 정확한 거리를 계산해 반경 밖을 제외하고 가까운 순 정렬
    // @throws CustomException {@link ErrorCode#INVALID_SEARCH_CONDITION} 조건이 하나도 없거나, 좌표 3종(lat/lng/radius) 중 일부만 왔거나, radius가 0 이하이거나, lat/lng가 유효 범위(-90~90/-180~180)를 벗어난 경우
    public Page<PlaceSearchResponse> searchPlaces(String keyword, String cat1, String cat2,
            String region, Double lat, Double lng, Double radiusMeters, Pageable pageable) {
        keyword = blankToNull(keyword);
        cat1 = blankToNull(cat1);
        cat2 = blankToNull(cat2);
        region = blankToNull(region);

        boolean anyGeo = lat != null || lng != null || radiusMeters != null;
        boolean allGeo = lat != null && lng != null && radiusMeters != null;
        // radius > 0만 확인하고 lat/lng 자체의 유효 범위(-90~90, -180~180)는 확인하지 않으면
        // 범위 밖 좌표가 통과돼 경계상자/Haversine 거리 계산이 무의미해짐
        boolean invalidGeoRange = allGeo && (lat < -90 || lat > 90 || lng < -180 || lng > 180);
        if (anyGeo && (!allGeo || radiusMeters <= 0 || invalidGeoRange)) {
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

        List<PlaceSearchResponse> all;
        if (!allGeo) {
            all = candidates.stream()
                    .map(PlaceSearchResponse::from)
                    .toList();
        } else {
            final double centerLat = lat;
            final double centerLng = lng;
            final double radius = radiusMeters;
            all = candidates.stream()
                    .map(place -> PlaceSearchResponse.of(place,
                            GeoUtil.distanceMeters(centerLat, centerLng, place.getLatitude(), place.getLongitude())))
                    .filter(response -> response.getDistanceMeters() <= radius)
                    .sorted(Comparator.comparing(PlaceSearchResponse::getDistanceMeters))
                    .limit(MAX_GEO_SEARCH_RESULTS)
                    .toList();
        }

        // 거리 계산/정렬을 애플리케이션에서 처리하므로 전체 목록을 만든 뒤 요청 페이지만 잘라 반환
        int start = (int) pageable.getOffset();
        if (start >= all.size()) {
            return new PageImpl<>(List.of(), pageable, all.size());
        }
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }

    // 빈 문자열/공백만 있는 파라미터를 null("조건 없음")로 통일
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    // 검색창 자동완성 — 빈 키워드는 빈 목록으로 처리(에러 대신 조용히 무시)
    public List<PlaceAutocompleteResponse> getAutocomplete(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return placeRepository.findTop8ByPlaceNameContainingOrderByPlaceNameAsc(keyword.trim()).stream()
                .map(PlaceAutocompleteResponse::from)
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

    // 찜 많은 순 장소 랭킹. 집계 쿼리가 [Place, 찜 개수] 쌍의 Object[]로 반환하므로
    // 각 자리 타입을 아는 여기서 캐스팅해 dto로 변환 (정렬은 쿼리에 고정돼 있어 Pageable의 sort는 안 쓰임)
    public Page<PlaceBookmarkCountResponse> getPlacesRankedByBookmarkCount(Pageable pageable) {
        return placeBookmarkRepository.findPlaceBookmarkCounts(pageable)
                .map(row -> PlaceBookmarkCountResponse.of((Place) row[0], (Long) row[1]));
    }

    // 평점 높은 순 장소 랭킹. 집계 쿼리가 [Place, 평균 별점] 쌍의 Object[]로 반환하므로
    // 각 자리 타입을 아는 여기서 캐스팅해 dto로 변환 (정렬은 쿼리에 고정돼 있어 Pageable의 sort는 안 쓰임)
    public Page<PlaceRatingResponse> getPlacesRankedByRating(Pageable pageable) {
        return reviewRepository.findPlacesOrderByAverageRating(pageable)
                .map(row -> PlaceRatingResponse.of((Place) row[0], (Double) row[1]));
    }
}
