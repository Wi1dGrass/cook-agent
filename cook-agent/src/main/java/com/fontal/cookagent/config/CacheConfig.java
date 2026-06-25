package com.fontal.cookagent.config;

import com.fontal.cookagent.utils.CacheUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 缓存配置 — 启用基于 cook.cache.redis.enable 的 L1+L2 双层缓存。
 *
 * <p>redis.enable=false 或 Redis 不可用时，自动降级为纯 Caffeine 模式（由 CacheUtils 内部处理）。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    private final CacheProperties cacheProperties;

    /**
     * 注入全局 CacheUtils。
     *
     * <p>cook.cache.redis.enable=false 时不注入 StringRedisTemplate，CacheUtils 内部健康检查
     * 会捕获 NPE 自动标记不可用并降级为纯 Caffeine 模式。
     */
    @Bean
    public CacheUtils cacheUtils(StringRedisTemplate redisTemplate) {
        CacheProperties.Caffeine caffeine = cacheProperties.getCaffeine();
        CacheProperties.Redis redis = cacheProperties.getRedis();

        if (!redis.isEnable()) {
            log.info("Redis 缓存已显式禁用 (cook.cache.redis.enable=false)，使用纯 Caffeine");
            redisTemplate = null;
        } else {
            log.info("启用 Redis L2 缓存: ttl={}s, caffeine.maxSize={}",
                    redis.getTtlSeconds(), caffeine.getMaxSize());
        }

        return new CacheUtils(
                redisTemplate,
                caffeine.getMaxSize(),
                Duration.ofSeconds(caffeine.getExpireAfterWriteSeconds()),
                Duration.ofSeconds(redis.isEnable() ? redis.getTtlSeconds() : caffeine.getExpireAfterWriteSeconds()));
    }
}