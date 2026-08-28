package com.agrawalpulse.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet filter that captures correlation ID from request headers or generates one.
 * Propagates correlation ID to all downstream logs via MDC.
 * Adds correlation ID to response headers so clients can trace their request.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
  private static final String X_CORRELATION_ID = "X-Correlation-ID";
  private static final String X_TRACE_ID = "X-Trace-ID";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    long startTime = System.currentTimeMillis();

    // Get or generate correlation ID
    String correlationId = request.getHeader(X_CORRELATION_ID);
    if (correlationId == null || correlationId.isEmpty()) {
      correlationId = LogContext.generateCorrelationId();
    }

    // Get or generate trace ID
    String traceId = request.getHeader(X_TRACE_ID);
    if (traceId == null || traceId.isEmpty()) {
      traceId = LogContext.generateCorrelationId().replace("req-", "trace-");
    }

    // Put in MDC so all logs in this request have these IDs
    LogContext.setCorrelationId(correlationId);
    LogContext.setTraceId(traceId);
    LogContext.setRequestPath(request.getRequestURI());

    // Add to response so client can correlate their logs
    response.addHeader(X_CORRELATION_ID, correlationId);
    response.addHeader(X_TRACE_ID, traceId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      long responseTime = System.currentTimeMillis() - startTime;
      LogContext.setResponseTime(responseTime);

      log.info("HTTP {} {} - {} ms [{}]",
          request.getMethod(),
          request.getRequestURI(),
          responseTime,
          response.getStatus());

      LogContext.clearAll();
    }
  }
}
