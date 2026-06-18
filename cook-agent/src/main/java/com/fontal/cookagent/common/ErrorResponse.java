package com.fontal.cookagent.common;

import org.springframework.http.HttpStatus;

/**
 * 统一错误响应格式。
 */
public record ErrorResponse(
        String code,
        String message,
        Integer status
) {
    public static ErrorResponse of(ErrorCode errorCode, HttpStatus httpStatus) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), httpStatus.value());
    }

    public static ErrorResponse of(ErrorCode errorCode, String detail, HttpStatus httpStatus) {
        return new ErrorResponse(errorCode.getCode(), detail, httpStatus.value());
    }

    public static ErrorResponse of(BizException ex, HttpStatus httpStatus) {
        return new ErrorResponse(ex.getCode(), ex.getMessage(), httpStatus.value());
    }
}
