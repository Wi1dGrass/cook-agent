package com.fontal.cookagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cook.cache")
public class CacheProperties {

    private final Redis redis = new Redis();
    private final Caffeine caffeine = new Caffeine();

    @Data
    public static class Redis {
        private boolean enable = true;
        private long ttlSeconds = 3600;
    }

    @Data
    public static class Caffeine {
        private int maxSize = 1000;
        private long expireAfterWriteSeconds = 3600;
    }
}