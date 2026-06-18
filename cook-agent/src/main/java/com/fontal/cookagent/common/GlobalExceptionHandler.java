package com.fontal.cookagent.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器 — 统一返回 ErrorResponse JSON 格式。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ErrorResponse> handleBizException(BizException ex) {
        log.warn("业务异常: code={}, message={}", ex.getCode(), ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ex, HttpStatus.BAD_REQUEST));
    }

    /** 缺少请求参数（@RequestParam required=true 但未传） */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("缺少参数: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ErrorCode.PARAM_MISSING,
                        "缺少必填参数: " + ex.getParameterName(),
                        HttpStatus.BAD_REQUEST));
    }

    /** 请求体为空或格式错误 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体格式错误: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ErrorCode.PARAM_INVALID,
                        "请求体格式错误或为空",
                        HttpStatus.BAD_REQUEST));
    }

    /** 参数类型转换错误 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("参数类型错误: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ErrorCode.PARAM_INVALID,
                        "参数 " + ex.getName() + " 类型错误",
                        HttpStatus.BAD_REQUEST));
    }

    /** 资源不存在（Spring 默认 404 处理） */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ErrorCode.NOT_FOUND,
                        "请求的资源不存在: " + ex.getResourcePath(),
                        HttpStatus.NOT_FOUND));
    }

    /** 兜底：未捕获异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("未捕获异常", ex);
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR,
                        ex.getMessage() != null ? ex.getMessage() : "服务器内部错误",
                        HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
