package com.tails.common.exception;

import lombok.Getter;

// 서비스에서 의도적으로 던지는 예외 (ErrorCode를 담아 GlobalExceptionHandler로 전달)
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
