package com.fontal.cookagent.app.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fontal.cookagent.utils.CacheUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheUtilsTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private CacheUtils cacheUtils;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.hasKey(anyString())).thenReturn(true);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        cacheUtils = new CacheUtils(redisTemplate, 100, Duration.ofMinutes(10), Duration.ofMinutes(10));
    }

    // ==================== L1 测试 ====================

    @Test
    @DisplayName("put + get — L1 命中")
    void putAndGet() {
        cacheUtils.put("key1", "value1");

        String result = cacheUtils.get("key1", String.class, () -> "fallback");
        assertThat(result).isEqualTo("value1");
    }

    @Test
    @DisplayName("get — L1/L2 miss 时调用 loader 回源")
    void getWithLoaderWhenMiss() {
        String result = cacheUtils.get("key-miss", String.class, () -> "loaded-value");
        assertThat(result).isEqualTo("loaded-value");

        // 第二次应命中 L1
        String cached = cacheUtils.get("key-miss", String.class, () -> "should-not-call");
        assertThat(cached).isEqualTo("loaded-value");
    }

    @Test
    @DisplayName("get — loader 只调用一次")
    void loaderCalledOnlyOnce() {
        AtomicInteger callCount = new AtomicInteger(0);

        cacheUtils.get("counter-key", String.class, () -> {
            callCount.incrementAndGet();
            return "counted";
        });
        cacheUtils.get("counter-key", String.class, () -> {
            callCount.incrementAndGet();
            return "should-not-be-called";
        });

        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("evict — 删除后 miss 走 loader")
    void evictThenMiss() {
        cacheUtils.put("evict-key", "test-value");
        cacheUtils.evict("evict-key");

        String result = cacheUtils.get("evict-key", String.class, () -> "reloaded");
        assertThat(result).isEqualTo("reloaded");
    }

    @Test
    @DisplayName("getOptional — 命中和 miss")
    void getOptional() {
        cacheUtils.put("opt-key", 42);
        assertThat(cacheUtils.getOptional("opt-key", Integer.class)).hasValue(42);
        assertThat(cacheUtils.getOptional("opt-miss", String.class)).isEmpty();
    }

    @Test
    @DisplayName("put — null 值不写入")
    void putNullIgnored() {
        cacheUtils.put("null-key", null);
        String result = cacheUtils.get("null-key", String.class, () -> "default");
        assertThat(result).isEqualTo("default");
    }

    @Test
    @DisplayName("put — 自定义 TTL")
    void putWithCustomTtl() {
        cacheUtils.put("ttl-key", "with-ttl", 30, java.util.concurrent.TimeUnit.SECONDS);

        String result = cacheUtils.get("ttl-key", String.class, () -> "fallback");
        assertThat(result).isEqualTo("with-ttl");
    }

    @Test
    @DisplayName("不同类型值的读写")
    void differentTypes() {
        cacheUtils.put("int-key", 123);
        cacheUtils.put("string-key", "hello");
        cacheUtils.put("bool-key", true);

        assertThat(cacheUtils.get("int-key", Integer.class, () -> 0)).isEqualTo(123);
        assertThat(cacheUtils.get("string-key", String.class, () -> "")).isEqualTo("hello");
        assertThat(cacheUtils.get("bool-key", Boolean.class, () -> false)).isTrue();
    }

    // ==================== L2 (Redis) 测试 ====================

    @Test
    @DisplayName("L2 命中 — 从 Redis 读取并回填 L1")
    void l2HitReadsFromRedis() throws JsonProcessingException {
        String json = mapper.writeValueAsString("redis-value");
        when(valueOps.get("redis-key")).thenReturn(json);

        String result = cacheUtils.get("redis-key", String.class, () -> "fallback");
        assertThat(result).isEqualTo("redis-value");

        // 第二次应从 L1 返回（回填验证）
        String cached = cacheUtils.get("redis-key", String.class, () -> "should-not-call");
        assertThat(cached).isEqualTo("redis-value");
    }

    @Test
    @DisplayName("Redis 不可用 — 自动降级为纯 Caffeine")
    void redisUnavailableAutoDegrade() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis down"));

        CacheUtils degraded = new CacheUtils(redisTemplate);
        assertThat(degraded.isRedisAvailable()).isFalse();

        // 仍能正常读写（纯 Caffeine 模式）
        degraded.put("degrade-key", "degraded-value");
        String result = degraded.get("degrade-key", String.class, () -> "fallback");
        assertThat(result).isEqualTo("degraded-value");
    }

    @Test
    @DisplayName("Redis 运行时失败 — 降级写入 Caffeine")
    void redisFailAtRuntime() {
        doThrow(new RuntimeException("Redis write error"))
                .when(valueOps).set(eq("fail-key"), anyString());

        // 写入应降级，但不抛异常
        cacheUtils.put("fail-key", "caffeine-only");
        assertThat(cacheUtils.isRedisAvailable()).isFalse();

        // 从 Caffeine 仍可取到
        String result = cacheUtils.get("fail-key", String.class, () -> "fallback");
        assertThat(result).isEqualTo("caffeine-only");
    }
}
