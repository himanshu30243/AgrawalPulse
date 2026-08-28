package com.agrawalpulse.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RestClient interceptor that applies Resilience4j patterns to all inter-service calls.
 *
 * Pattern: CircuitBreaker → Retry → Execute
 *
 * - Circuit Breaker: Opens if 50% error rate over 10 calls, waits 30s before retrying
 * - Retry: 2 attempts for connection/socket timeouts
 * - Timeout: Configured via RestClient client itself (no interceptor-level timeout)
 */
@Component
public class ResilienceInterceptor implements ClientHttpRequestInterceptor {
  private static final Logger log = LoggerFactory.getLogger(ResilienceInterceptor.class);
  private static final String UNKNOWN_SERVICE = "unknown-service";

  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final RetryRegistry retryRegistry;

  public ResilienceInterceptor(
      CircuitBreakerRegistry circuitBreakerRegistry,
      RetryRegistry retryRegistry) {
    this.circuitBreakerRegistry = circuitBreakerRegistry;
    this.retryRegistry = retryRegistry;
  }

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
      throws IOException {

    String serviceName = extractServiceName(request.getURI().getHost());

    try {
      CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
      Retry retry = retryRegistry.retry(serviceName);

      log.debug("Calling {} with circuitBreaker={}, retry={}",
          request.getURI(),
          circuitBreaker.getState(),
          retry.getRetryConfig().getMaxAttempts());

      // Apply circuit breaker
      return circuitBreaker.executeSupplier(() ->
          // Apply retry
          retry.executeSupplier(() -> {
            try {
              return execution.execute(request, body);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }));

    } catch (CallNotPermittedException e) {
      log.warn("Circuit breaker OPEN for {}: Service appears to be down.", serviceName);
      throw new IOException("Service temporarily unavailable (circuit breaker open)", e);

    } catch (RuntimeException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }
      log.error("Resilience error calling {}: {}", request.getURI(), e.getMessage(), e);
      throw new IOException("Inter-service call failed", e);
    }
  }

  private String extractServiceName(String host) {
    if (host == null || host.equals("localhost") || host.equals("127.0.0.1")) {
      return UNKNOWN_SERVICE;
    }

    // Extract service name from host (e.g., "user-service" from "user-service:8081")
    return host.split(":")[0];
  }
}
