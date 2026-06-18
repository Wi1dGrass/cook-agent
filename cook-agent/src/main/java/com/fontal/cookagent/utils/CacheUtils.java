package com.fontal.cookagent.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Caffeine L1 + Redis L2 统一缓存工具类。
 *
 * 读路径：L1 命中 → 返回；L1 miss → L2 查询 → 回填 L1；L2 miss → loader 回源 → 写入 L2 + L1
 * 写路径：先写 L2（Redis），再写 L1（Caffeine）
 * Redis 不可用时自动降级为纯 Caffeine 模式，不抛异常。
 */
@Slf4j
public class CacheUtils {

    private final Cache<String, Object> caffeine;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private volatile boolean redisAvailable = true;

    public CacheUtils(StringRedisTemplate redis) {
        this(redis, 1000, Duration.ofHours(1), Duration.ofHours(1));
    }

    public CacheUtils(StringRedisTemplate redis, int maxSize, Duration caffeineTtl, Duration redisTtl) {
        this.redis = redis;
        this.caffeine = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(caffeineTtl)
                .build();
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        // 启动时检测 Redis 连通性
        try {
            redis.hasKey("__cache_health_check__");
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("Redis 连接不可用，CacheUtils 降级为纯 Caffeine 模式");
        }
    }

    // ==================== 读 ====================

    /** 从缓存获取，miss 时调用 loader 回源并自动写入缓存 */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type, Supplier<T> loader) {
        // L1: Caffeine
        Object l1Value = caffeine.getIfPresent(key);
        if (l1Value != null) {
            log.debug("L1 命中: {}", key);
            return (T) l1Value;
        }

        // L2: Redis
        if (redisAvailable) {
            try {
                String json = redis.opsForValue().get(key);
                if (json != null) {
                    T value = mapper.readValue(json, type);
                    caffeine.put(key, value); // 回填 L1
                    log.debug("L2 命中: {}", key);
                    return value;
                }
            } catch (Exception e) {
                markRedisUnavailable(e);
            }
        }

        // 回源
        T value = loader.get();
        if (value != null) {
            caffeine.put(key, value);
            writeToRedis(key, value);
        }
        return value;
    }

    /** 获取并返回 Optional */
    public <T> Optional<T> getOptional(String key, Class<T> type) {
        T value = get(key, type, () -> null);
        return Optional.ofNullable(value);
    }

    // ==================== 写 ====================

    /** 写入两级缓存（使用默认 TTL） */
    public void put(String key, Object value) {
        if (value == null) return;
        caffeine.put(key, value);
        writeToRedis(key, value);
    }

    /** 写入两级缓存（指定 Redis TTL） */
    public void put(String key, Object value, long ttl, TimeUnit unit) {
        if (value == null) return;
        caffeine.put(key, value);
        writeToRedis(key, value, ttl, unit);
    }

    // ==================== 删除 ====================

    /** 同步删除两级缓存 */
    public void evict(String key) {
        caffeine.invalidate(key);
        if (redisAvailable) {
            try {
                redis.delete(key);
            } catch (Exception e) {
                markRedisUnavailable(e);
            }
        }
    }

    /** 清空所有缓存 */
    public void evictAll() {
        caffeine.invalidateAll();
        if (redisAvailable) {
            try {
                var keys = redis.keys("*");
                if (keys != null && !keys.isEmpty()) {
                    redis.delete(keys);
                }
            } catch (Exception e) {
                markRedisUnavailable(e);
            }
        }
    }

    // ==================== 内部 ====================

    private void writeToRedis(String key, Object value) {
        if (!redisAvailable) return;
        try {
            String json = mapper.writeValueAsString(value);
            redis.opsForValue().set(key, json);
        } catch (JsonProcessingException e) {
            log.warn("缓存值 JSON 序列化失败: {}", key, e);
        } catch (Exception e) {
            markRedisUnavailable(e);
        }
    }

    private void writeToRedis(String key, Object value, long ttl, TimeUnit unit) {
        if (!redisAvailable) return;
        try {
            String json = mapper.writeValueAsString(value);
            redis.opsForValue().set(key, json, ttl, unit);
        } catch (JsonProcessingException e) {
            log.warn("缓存值 JSON 序列化失败: {}", key, e);
        } catch (Exception e) {
            markRedisUnavailable(e);
        }
    }

    private void markRedisUnavailable(Exception e) {
        if (redisAvailable) {
            redisAvailable = false;
            log.warn("Redis 操作失败，降级为纯 Caffeine 模式: {}", e.getMessage());
        }
    }

    // ==================== 暴露指标 ====================

    public long caffeineSize() {
        caffeine.cleanUp();
        return caffeine.estimatedSize();
    }

    public boolean isRedisAvailable() {
        return redisAvailable;
    }
}
