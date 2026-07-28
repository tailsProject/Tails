package com.tails.place.sync;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tails.place.sync.dto.PetTourDetailItem;
import com.tails.place.sync.dto.PetTourImageItem;
import com.tails.place.sync.dto.PetTourListItem;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// KorPetTourService2의 petTourSyncList2(목록)/detailPetTour2(상세) 오퍼레이션 클라이언트
// 두 오퍼레이션 응답이 같은 envelope 구조를 공유해서, item 타입만 다른 제네릭 record로
// 내부에 감춰둠(다른 클래스에서 쓸 일 없는 구현 세부사항이라 dto 패키지로 안 뺐음)
@Component
public class PetTourApiClient {

    private static final String BASE_URL = "http://apis.data.go.kr/B551011/KorPetTourService2";
    private static final String MOBILE_OS = "ETC";
    private static final String MOBILE_APP = "Tails";
    static final String SUCCESS_RESULT_CODE = "0000";

    // 연결/응답 타임아웃 없으면 TourAPI가 응답을 지연시키거나 연결만 맺어둔 채 멈출 때
    // 동기화 요청(최대 100건씩 반복 호출) 스레드가 무한정 블로킹될 수 있음
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final TourApiProperties tourApiProperties;

    public PetTourApiClient(TourApiProperties tourApiProperties) {
        // 기본 HttpClient는 http(평문)여도 h2c 업그레이드를 시도하는데, TourAPI 게이트웨이가
        // 이걸 못 받아줘서 502발생. HTTP/1.1로 고정하면 정상 응답
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        // 상세정보가 없는 항목은 TourAPI가 "items":{} 대신 "items":"" (빈 문자열)로 내려줘서,
        // 이 옵션이 없으면 TourApiItems로 역직렬화하다가 예외가 나 fetchDetail 전체가 실패 처리
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .messageConverters(converters -> converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper)))
                .build();
        this.tourApiProperties = tourApiProperties;
    }

    // contentTypeId가 null이면 전체 카테고리, areaCode가 null이면 전국 대상으로 조회
    // areaCode: 시도 단위 지역코드(예: 1=서울, 32=강원, 39=제주 등) - 카테고리 하나가 특정
    // 지역에 쏠려있을 때, 지역별로 나눠 받아 균등한 샘플을 뽑고 싶은 경우에 사용
    public List<PetTourListItem> fetchSyncList(String contentTypeId, String areaCode, int pageNo, int numOfRows) {
        StringBuilder url = new StringBuilder(BASE_URL)
                .append("/petTourSyncList2")
                .append("?serviceKey=").append(tourApiProperties.serviceKey())
                .append("&MobileOS=").append(MOBILE_OS)
                .append("&MobileApp=").append(MOBILE_APP)
                .append("&_type=json")
                .append("&numOfRows=").append(numOfRows)
                .append("&pageNo=").append(pageNo);

        if (contentTypeId != null) {
            url.append("&contentTypeId=").append(contentTypeId);
        }
        if (areaCode != null) {
            url.append("&areaCode=").append(areaCode);
        }

        TourApiBody<PetTourListItem> body = fetchBody(
                url.toString(), new ParameterizedTypeReference<TourApiEnvelope<PetTourListItem>>() {
                });

        return body.items() == null ? List.of() : body.items().item();
    }

    // totalCount만 필요해서 numOfRows=1로 최소 조회. item 내용은 안 쓰므로 타입은 Object
    public long fetchTotalCount(String contentTypeId) {
        String url = BASE_URL + "/petTourSyncList2"
                + "?serviceKey=" + tourApiProperties.serviceKey()
                + "&MobileOS=" + MOBILE_OS
                + "&MobileApp=" + MOBILE_APP
                + "&_type=json"
                + "&numOfRows=1"
                + "&pageNo=1"
                + "&contentTypeId=" + contentTypeId;

        TourApiBody<Object> body = fetchBody(url, new ParameterizedTypeReference<TourApiEnvelope<Object>>() {
        });

        return body.totalCount();
    }

    // 결과가 1건이어도 item이 배열로 내려와서 첫 번째 값을 꺼냄
    // 반려동물 동반 상세정보가 없는 contentId는 items가 아예 빈 문자열로 내려와서 null 반환
    public PetTourDetailItem fetchDetail(String contentId) {
        String url = BASE_URL + "/detailPetTour2"
                + "?serviceKey=" + tourApiProperties.serviceKey()
                + "&MobileOS=" + MOBILE_OS
                + "&MobileApp=" + MOBILE_APP
                + "&_type=json"
                + "&contentId=" + contentId;

        TourApiBody<PetTourDetailItem> body = fetchBody(
                url, new ParameterizedTypeReference<TourApiEnvelope<PetTourDetailItem>>() {
                });

        return body.items() == null ? null : body.items().item().getFirst();
    }

    // 장소 하나에 딸린 추가 사진 목록 조회. 사진이 없는 항목은 detailPetTour2와 마찬가지로
    // items가 빈 문자열로 내려와서 null 처리됨(ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
    public List<PetTourImageItem> fetchImages(String contentId) {
        String url = BASE_URL + "/detailImage2"
                + "?serviceKey=" + tourApiProperties.serviceKey()
                + "&MobileOS=" + MOBILE_OS
                + "&MobileApp=" + MOBILE_APP
                + "&_type=json"
                + "&contentId=" + contentId
                + "&imageYN=Y";

        TourApiBody<PetTourImageItem> body = fetchBody(
                url, new ParameterizedTypeReference<TourApiEnvelope<PetTourImageItem>>() {
                });

        return body.items() == null ? List.of() : body.items().item();
    }

    // HTTP 호출 + envelope 해체 + resultCode 검증 공통 처리
    private <T> TourApiBody<T> fetchBody(String url, ParameterizedTypeReference<TourApiEnvelope<T>> typeRef) {
        TourApiEnvelope<T> envelope = restClient.get()
                .uri(URI.create(url))
                .retrieve()
                .body(typeRef);

        TourApiHeader header = envelope.response().header();
        if (!SUCCESS_RESULT_CODE.equals(header.resultCode())) {
            throw new IllegalStateException(
                    "TourAPI 호출 실패. url=" + url
                            + ", resultCode=" + header.resultCode()
                            + ", resultMsg=" + header.resultMsg());
        }

        return envelope.response().body();
    }

    private record TourApiEnvelope<T>(TourApiResponse<T> response) {
    }

    private record TourApiResponse<T>(TourApiHeader header, TourApiBody<T> body) {
    }

    private record TourApiHeader(String resultCode, String resultMsg) {
    }

    private record TourApiBody<T>(TourApiItems<T> items, int numOfRows, int pageNo, long totalCount) {
    }

    private record TourApiItems<T>(List<T> item) {
    }
}
