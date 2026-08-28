package com.agrawalpulse.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Custom gateway filter that:
 * 1. Adds/extracts correlation ID from X-Correlation-ID header
 * 2. Logs all requests/responses with correlation ID
 * 3. Propagates correlation ID to all downstream services
 *
 * Every request flows through this filter automatically.
 */
@Component
public class RequestLoggingGatewayFilterFactory
    extends AbstractGatewayFilterFactory<RequestLoggingGatewayFilterFactory.Config> {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingGatewayFilterFactory.class);
  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  private static final String TRACE_ID_HEADER = "X-Trace-ID";

  public RequestLoggingGatewayFilterFactory() {
    super(Config.class);
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      ServerHttpRequest request = exchange.getRequest();

      // Get or create correlation ID
      String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
      if (correlationId == null || correlationId.isEmpty()) {
        correlationId = "req-" + UUID.randomUUID().toString().substring(0, 8);
      }

      // Get or create trace ID
      String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
      if (traceId == null || traceId.isEmpty()) {
        traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
      }

      final String finalCorrelationId = correlationId;
      final String finalTraceId = traceId;

      // Log incoming request
      long startTime = System.currentTimeMillis();
      log.info("GATEWAY -> {} {} [correlationId={}]",
          request.getMethod(),
          request.getURI().getPath(),
          finalCorrelationId);

      // Create new request with correlation ID headers
      ServerHttpRequest mutatedRequest = request.mutate()
          .header(CORRELATION_ID_HEADER, finalCorrelationId)
          .header(TRACE_ID_HEADER, finalTraceId)
          .build();

      // Continue chain with mutated request
      return chain.filter(exchange.mutate().request(mutatedRequest).build())
          .then(Mono.fromRunnable(() -> {
            // Log outgoing response
            long duration = System.currentTimeMillis() - startTime;
            ServerHttpResponse response = exchange.getResponse();
            log.info("GATEWAY <- {} {} - {} ms [status={}]",
                request.getMethod(),
                request.getURI().getPath(),
                duration,
                response.getStatusCode());
          }));
    };
  }

  /**
   * Configuration class for RequestLoggingGatewayFilterFactory.
   * Can be extended to support filter-specific parameters in route config.
   */
  public static class Config {
    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
