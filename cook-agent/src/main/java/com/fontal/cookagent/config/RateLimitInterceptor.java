package com.fontal.cookagent.config;

import com.fontal.cookagent.common.ErrorCode;
import com.fontal.cookagent.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流拦截器 — 基于 ConcurrentHashMap 的滑动窗口计数。
 * key = IP + methodName，value = (windowStart, count)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    /** key → WindowCounter */
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String ip = getClientIp(request);
        String methodKey = handlerMethod.getMethod().getName();
        String key = ip + ":" + methodKey;

        int limit = rateLimit.limit();
        int windowSeconds = rateLimit.windowSeconds();
        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;

        WindowCounter counter = counters.compute(key, (k, v) -> {
            if (v == null || now - v.windowStart > windowMs) {
                return new WindowCounter(now);
            }
            return v;
        });

        int count = counter.count.incrementAndGet();

        if (count > limit) {
            log.warn("限流触发: ip={}, method={}, count={}/{}, window={}s",
                    ip, methodKey, count, limit, windowSeconds);
            writeRateLimitResponse(response);
            return false;
        }

        return true;
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse error = new ErrorResponse(
                ErrorCode.RATE_LIMITED.getCode(),
                ErrorCode.RATE_LIMITED.getMessage(),
                HttpStatus.TOO_MANY_REQUESTS.value()
        );
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /** 滑动窗口计数器 */
    private static class WindowCounter {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        WindowCounter(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
