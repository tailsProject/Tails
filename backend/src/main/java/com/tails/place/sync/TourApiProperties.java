package com.tails.place.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;

// tour-api.* 설정값. @Component 대신 @ConfigurationPropertiesScan으로 등록 —
// @Component로 등록하면 생성자 바인딩이 안 먹어서 구동 시 빈을 못 찾아 실패
@ConfigurationProperties(prefix = "tour-api")
public record TourApiProperties(String serviceKey) {
}
