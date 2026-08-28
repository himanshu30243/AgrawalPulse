package com.agrawalpulse.logging;

import org.slf4j.MDC;
import java.util.UUID;

/**
 * Utility for managing structured logging context (Mapped Diagnostic Context).
 * Used to add correlation IDs, user IDs, tenant IDs to all logs across service boundaries.
 */
public class LogContext {
  private static final String TRACE_ID = "traceId";
  private static final String CORRELATION_ID = "correlationId";
  private static final String USER_ID = "userId";
  private static final String TENANT_ID = "tenantId";
  private static final String REQUEST_PATH = "requestPath";
  private static final String RESPONSE_TIME_MS = "responseTimeMs";

  public static void setTraceId(String traceId) {
    MDC.put(TRACE_ID, traceId);
  }

  public static void setCorrelationId(String correlationId) {
    MDC.put(CORRELATION_ID, correlationId);
  }

  public static void setUserId(String userId) {
    MDC.put(USER_ID, userId);
  }

  public static void setTenantId(String tenantId) {
    MDC.put(TENANT_ID, tenantId);
  }

  public static void setRequestPath(String path) {
    MDC.put(REQUEST_PATH, path);
  }

  public static void setResponseTime(long millis) {
    MDC.put(RESPONSE_TIME_MS, String.valueOf(millis));
  }

  public static String getTraceId() {
    return MDC.get(TRACE_ID);
  }

  public static String getCorrelationId() {
    return MDC.get(CORRELATION_ID);
  }

  public static String getUserId() {
    return MDC.get(USER_ID);
  }

  public static String getTenantId() {
    return MDC.get(TENANT_ID);
  }

  public static void clearAll() {
    MDC.clear();
  }

  public static String generateCorrelationId() {
    return "req-" + UUID.randomUUID().toString().substring(0, 8);
  }
}
