package com.tails.place.sync;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import java.util.Arrays;
import lombok.Getter;

// KorPetTourService2의 contentTypeId(관광 콘텐츠 대분류) 코드 + label
@Getter
public enum PetTourContentType {

    TOURIST_SPOT("12", "관광지"),
    CULTURAL_FACILITY("14", "문화시설"),
    FESTIVAL("15", "행사공연축제"),
    LEISURE_SPORTS("28", "레포츠"),
    ACCOMMODATION("32", "숙박"),
    SHOPPING("38", "쇼핑"),
    RESTAURANT("39", "음식점");

    private final String code;
    private final String label;

    PetTourContentType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    // CustomException으로 던져야 GlobalExceptionHandler가 400으로 응답(IllegalArgumentException은 500 처리됨)
    public static PetTourContentType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
    }
}
