package com.fontal.cookagent.config;

import java.lang.annotation.*;

/**
 * 接口限流注解 — 标记在 Controller 方法上。
 * 使用 Caffeine 本地缓存实现滑动窗口计数。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 时间窗口内允许的最大请求数，默认 30 */
    int limit() default 30;

    /** 时间窗口（秒），默认 60 秒 */
    int windowSeconds() default 60;
}
