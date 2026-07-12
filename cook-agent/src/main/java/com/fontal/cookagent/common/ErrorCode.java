package com.fontal.cookagent.common;

/**
 * 业务错误码枚举。
 */
public enum ErrorCode {

    // ===== 通用 =====
    SUCCESS("SUCCESS", "成功"),
    PARAM_INVALID("PARAM_INVALID", "参数错误"),
    PARAM_MISSING("PARAM_MISSING", "缺少必填参数"),
    NOT_FOUND("NOT_FOUND", "资源不存在"),
    UNAUTHORIZED("UNAUTHORIZED", "未登录或登录已过期"),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "请求方法不支持"),
    INTERNAL_ERROR("INTERNAL_ERROR", "服务器内部错误"),

    // ===== Agent =====
    AGENT_ERROR("AGENT_ERROR", "Agent 执行出错"),
    AGENT_BUSY("AGENT_BUSY", "Agent 正在执行中"),

    // ===== 搜索 =====
    SEARCH_UNAVAILABLE("SEARCH_UNAVAILABLE", "搜索服务暂不可用"),
    SEARCH_NO_RESULTS("SEARCH_NO_RESULTS", "未找到搜索结果"),

    // ===== 限流 =====
    RATE_LIMITED("RATE_LIMITED", "请求过于频繁，请稍后重试"),

    // ===== 校验 =====
    VALIDATION_ERROR("VALIDATION_ERROR", "参数校验不通过"),

    ;

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
