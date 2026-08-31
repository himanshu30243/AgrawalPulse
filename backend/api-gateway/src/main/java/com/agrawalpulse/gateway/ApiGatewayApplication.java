package com.agrawalpulse.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * API Gateway - Single entry point for all AgrawalPulse microservices.
 *
 * Routes client requests to:
 *   - user-service (port 8081)
 *   - family-service (port 8082)
 *   - membership-service (port 8083)
 *   - matrimony-service (port 8084)
 *   - event-service (port 8085)
 *   - analytics-service (port 8086)
 *
 * Handles cross-cutting concerns:
 *   - JWT authentication (via common module)
 *   - Rate limiting (via Resilience4j)
 *   - Circuit breaker at gateway level
 *   - CORS headers
 *   - Request logging with correlation IDs
 *   - Request/response transformation
 *
 * Runs on port 8080 by default.
 */
// Only pull in the reactive-safe pieces of common (resilience, cache). common.logging's
// RequestLoggingFilter is a servlet Filter (jakarta.servlet/OncePerRequestFilter) and
// com.agrawalpulse.common is servlet/Spring-MVC-based security config - both are built for the
// other (servlet) services and are incompatible with this module's reactive WebFlux/Gateway
// runtime, whose own request logging is handled by RequestLoggingGatewayFilterFactory instead.
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.agrawalpulse.gateway",
    "com.agrawalpulse.resilience",
    "com.agrawalpulse.cache"
})
public class ApiGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}
