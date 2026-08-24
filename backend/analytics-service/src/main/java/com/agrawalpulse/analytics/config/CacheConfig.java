package com.agrawalpulse.analytics.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

// Lives here rather than in common: it's Redis-specific (RedisCacheManagerBuilderCustomizer),
// and common has no Redis dependency since analytics-service is the only service using
// @Cacheable - pulling Redis into common would force it onto every other service for nothing.
@Configuration
public class CacheConfig {

    // Real ElastiCache/Redis, used everywhere except local dev (see the "local" bean below).
    @Bean
    @Profile("!local")
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        // Analytics DTOs are plain records (not java.io.Serializable), so cache values are
        // JSON-serialized rather than relying on Redis's default JDK serialization.
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        return builder -> builder.cacheDefaults(defaultConfig);
    }

    // Local dev has no Redis running (no Docker/WSL requirement to work on this service). A
    // plain in-process CacheManager satisfies every @Cacheable call site with the same cache
    // names, and Spring Boot's Redis auto-configuration backs off entirely once any CacheManager
    // bean is present - so this also means no connection is ever attempted to localhost:6379.
    @Bean
    @Profile("local")
    public CacheManager localCacheManager() {
        return new ConcurrentMapCacheManager(
                "analytics:familyTotals",
                "analytics:activeMemberships",
                "analytics:marriageReadiness",
                "analytics:population",
                "analytics:populationByCity",
                "analytics:populationByState",
                "analytics:summary");
    }
}
