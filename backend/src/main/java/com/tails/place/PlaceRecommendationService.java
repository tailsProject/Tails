package com.tails.place;

import com.tails.bookmark.PlaceBookmarkRepository;
import com.tails.place.dto.PlaceRecommendationResponse;
import com.tails.place.dto.PlaceResponse;
import com.tails.review.Review;
import com.tails.review.ReviewRepository;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 콘텐츠 기반(content-based) 장소 추천. 회원이 찜/리뷰한 장소들의 카테고리(cat1/cat2/cat3)로
// "취향 벡터"를 만들고, 아직 상호작용하지 않은 장소와 코사인 유사도를 계산해 높은 순으로 추천
// (협업 필터링 대신 콘텐츠 기반을 쓴 이유: 사용자 수가 적은 초기 단계라 "다른 사람 데이터"가
// 부족해도 이 회원 한 명의 이력만으로 동작함 — 콜드 스타트 문제 회피)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceRecommendationService {

    private static final int RECOMMENDATION_SIZE = 10;
    // 이력이 너무 많은 회원이어도 최근 것 위주로만 취향을 반영하기 위한 상한
    private static final int INTERACTION_SAMPLE_SIZE = 100;

    private final PlaceBookmarkRepository placeBookmarkRepository;
    private final ReviewRepository reviewRepository;
    private final PlaceRepository placeRepository;

    // 이 회원에게 추천할 장소 목록을 유사도 높은 순으로 반환. 찜/리뷰 이력이 하나도 없으면
    // 취향을 계산할 근거가 없으므로 빈 목록 반환 (인기순 등으로 억지로 대체하지 않음)
    public List<PlaceRecommendationResponse> recommend(Long memberId) {
        Pageable sample = PageRequest.of(0, INTERACTION_SAMPLE_SIZE);
        Set<Long> interactedPlaceIds = new HashSet<>();
        Map<String, Double> memberVector = new HashMap<>();

        for (Place place : placeBookmarkRepository.findBookmarkedPlacesByMemberId(memberId, sample)) {
            interactedPlaceIds.add(place.getPlaceId());
            accumulate(memberVector, place);
        }
        for (Review review : reviewRepository.findByMemberIdWithPlace(memberId, sample)) {
            Place place = review.getPlace();
            interactedPlaceIds.add(place.getPlaceId());
            accumulate(memberVector, place);
        }

        if (memberVector.isEmpty()) {
            return List.of();
        }

        return placeRepository.findAll().stream()
                .filter(place -> !interactedPlaceIds.contains(place.getPlaceId()))
                .map(place -> new AbstractMap.SimpleEntry<>(place, cosineSimilarity(memberVector, categoryVector(place))))
                // 카테고리가 하나도 안 겹치면 유사도 0 - 추천 의미가 없으니 제외
                .filter(entry -> entry.getValue() > 0)
                .sorted(Comparator.comparingDouble((Map.Entry<Place, Double> entry) -> entry.getValue()).reversed())
                .limit(RECOMMENDATION_SIZE)
                .map(entry -> new PlaceRecommendationResponse(PlaceResponse.from(entry.getKey()), entry.getValue()))
                .toList();
    }

    // 회원 취향 벡터에 이 장소의 카테고리들을 더함 (등장할 때마다 +1 - 몇 번 좋아했는지가 취향의 강도)
    private void accumulate(Map<String, Double> vector, Place place) {
        for (String dimension : categoryDimensions(place)) {
            vector.merge(dimension, 1.0, Double::sum);
        }
    }

    // 장소 하나의 카테고리 벡터. 회원 벡터와 달리 장소 자신은 각 카테고리를 한 번씩만 가짐
    private Map<String, Double> categoryVector(Place place) {
        Map<String, Double> vector = new HashMap<>();
        for (String dimension : categoryDimensions(place)) {
            vector.put(dimension, 1.0);
        }
        return vector;
    }

    // cat1/cat2/cat3를 "cat1:A001"처럼 레벨 접두사를 붙여 서로 다른 차원으로 구분
    // (접두사가 없으면 레벨이 다른데 코드값이 우연히 같은 경우 같은 차원으로 잘못 합쳐짐). null 레벨은 제외
    private List<String> categoryDimensions(Place place) {
        List<String> dimensions = new ArrayList<>();
        if (place.getCat1() != null) {
            dimensions.add("cat1:" + place.getCat1());
        }
        if (place.getCat2() != null) {
            dimensions.add("cat2:" + place.getCat2());
        }
        if (place.getCat3() != null) {
            dimensions.add("cat3:" + place.getCat3());
        }
        return dimensions;
    }

    // 두 벡터의 코사인 유사도(0~1) - 방향이 같을수록(카테고리 구성이 비슷할수록) 1에 가까움
    private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0;
        for (Map.Entry<String, Double> entry : a.entrySet()) {
            Double bValue = b.get(entry.getKey());
            if (bValue != null) {
                dot += entry.getValue() * bValue;
            }
        }
        double normA = Math.sqrt(a.values().stream().mapToDouble(v -> v * v).sum());
        double normB = Math.sqrt(b.values().stream().mapToDouble(v -> v * v).sum());
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (normA * normB);
    }
}
