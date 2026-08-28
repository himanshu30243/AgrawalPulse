package com.agrawalpulse.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for inter-service communication.
 * Applies: circuit breaker, retry, timeout patterns to all RestClient calls.
 *
 * Timeouts: 3s (user-service), 5s (other services)
 * Retry: 2 attempts for 5xx errors
 * Circuit breaker: opens after 50% error rate over 10 requests
 */
@Configuration
public class ResilienceConfig {
  private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry() {
    CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
        .slidingWindowSize(10)
        .failureRateThreshold(50.0f)
        .slowCallRateThreshold(50.0f)
        .slowCallDurationThreshold(Duration.ofSeconds(2))
        .waitDurationInOpenState(Duration.ofSeconds(30))
        .permittedNumberOfCallsInHalfOpenState(3)
        .automaticTransitionFromOpenToHalfOpenEnabled(true)
        .recordExceptions(Exception.class)
        .ignoreExceptions(IllegalArgumentException.class)
        .build();

    CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);

    registry.getEventPublisher()
        .onEntryAdded(event -> log.info("Circuit breaker created: {}", event.getAddedEntry().getName()))
        .onEntryRemoved(event -> log.info("Circuit breaker removed: {}", event.getRemovedEntry().getName()));

    return registry;
  }

  @Bean
  public RetryRegistry retryRegistry() {
    RetryConfig defaultConfig = RetryConfig.custom()
        .maxAttempts(2)
        .waitDuration(Duration.ofMillis(100))
        .retryExceptions(java.net.ConnectException.class, java.net.SocketTimeoutException.class)
        .ignoreExceptions(IllegalArgumentException.class)
        .build();

    RetryRegistry registry = RetryRegistry.of(defaultConfig);

    registry.getEventPublisher()
        .onEntryAdded(event -> log.info("Retry policy created: {}", event.getAddedEntry().getName()))
        .onEntryRemoved(event -> log.info("Retry policy removed: {}", event.getRemovedEntry().getName()));

    return registry;
  }

  @Bean
  public TimeLimiterRegistry timeLimiterRegistry() {
    TimeLimiterConfig defaultConfig = TimeLimiterConfig.custom()
        .timeoutDuration(Duration.ofSeconds(5))
        .cancelRunningFuture(true)
        .build();

    TimeLimiterRegistry registry = TimeLimiterRegistry.of(defaultConfig);

    registry.getEventPublisher()
        .onEntryAdded(event -> log.info("Time limiter created: {} (timeout: 5s)",
            event.getAddedEntry().getName()));

    return registry;
  }

  @Bean
  public CircuitBreaker userServiceCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("user-service",
        CircuitBreakerConfig.custom()
            .slidingWindowSize(15)
            .failureRateThreshold(40.0f)
            .waitDurationInOpenState(Duration.ofSeconds(20))
            .build());
  }

  @Bean
  public CircuitBreaker familyServiceCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("family-service");
  }

  @Bean
  public CircuitBreaker membershipServiceCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("membership-service");
  }

  @Bean
  public CircuitBreaker matrimonyServiceCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("matrimony-service");
  }

  @Bean
  public CircuitBreaker eventServiceCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("event-service");
  }

  @Bean
  public CircuitBreaker analyticsServiceCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("analytics-service");
  }

  @Bean
  public TimeLimiter userServiceTimeLimiter(TimeLimiterRegistry registry) {
    return registry.timeLimiter("user-service",
        TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(3))
            .build());
  }

  @Bean
  public TimeLimiter familyServiceTimeLimiter(TimeLimiterRegistry registry) {
    return registry.timeLimiter("family-service");
  }

  @Bean
  public TimeLimiter membershipServiceTimeLimiter(TimeLimiterRegistry registry) {
    return registry.timeLimiter("membership-service");
  }

  @Bean
  public TimeLimiter matrimonyServiceTimeLimiter(TimeLimiterRegistry registry) {
    return registry.timeLimiter("matrimony-service");
  }

  @Bean
  public TimeLimiter eventServiceTimeLimiter(TimeLimiterRegistry registry) {
    return registry.timeLimiter("event-service");
  }

  @Bean
  public TimeLimiter analyticsServiceTimeLimiter(TimeLimiterRegistry registry) {
    return registry.timeLimiter("analytics-service");
  }
}
