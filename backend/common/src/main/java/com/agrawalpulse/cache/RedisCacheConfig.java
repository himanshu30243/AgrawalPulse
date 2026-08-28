package com.agrawalpulse.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis Cache Configuration.
 *
 * Replaces in-memory ConcurrentMapCacheManager with distributed Redis cache.
 * Enables caching across multiple service instances.
 *
 * Benefits:
 *   - Shared cache across all instances (user-service-1, user-service-2, etc.)
 *   - Cluster-wide cache invalidation
 *   - Survives service restarts
 *   - Better performance for frequently accessed data
 *
 * Profiles:
 *   - local: Disabled (uses in-memory ConcurrentMapCacheManager)
 *   - dev/staging/prod: Enabled (uses Redis)
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisCacheConfig {

  private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    log.info("Initializing Redis Cache Manager");

    return RedisCacheManager.create(connectionFactory);
  }
}
