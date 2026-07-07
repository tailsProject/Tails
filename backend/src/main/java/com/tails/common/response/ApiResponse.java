package com.tails.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tails.common.exception.ErrorCode;

// 모든 API 응답을 {success, data, error} 하나의 모양으로 통일
public record ApiResponse<T>(@JsonProperty("success") boolean ok, T data, ErrorPayload error) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, new ErrorPayload(errorCode.name(), errorCode.getMessage()));
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorPayload(code, message));
    }

    public record ErrorPayload(String code, String message) {
    }
}
