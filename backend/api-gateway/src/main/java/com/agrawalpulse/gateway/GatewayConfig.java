package com.agrawalpulse.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Spring Cloud Gateway routing configuration.
 *
 * Defines routes from gateway (port 8080) to all microservices (ports 8081-8086).
 * Routes are patterns - all requests matching a path prefix are forwarded to the target service.
 *
 * Example:
 *   GET /api/v1/users → routes to user-service:8081/api/v1/users
 *   GET /api/v1/families → routes to family-service:8082/api/v1/families
 */
@Configuration
public class GatewayConfig {

  @Bean
  public RouteLocator routes(RouteLocatorBuilder builder) {
    return builder.routes()

        // User Service (port 8081)
        .route("user-service-route",
            r -> r.path("/api/v1/users/**", "/api/v1/local-auth/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Route", "user-service"))
                .uri("http://localhost:8081"))

        // Family Service (port 8082)
        .route("family-service-route",
            r -> r.path("/api/v1/families/**", "/api/v1/chapters/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Route", "family-service"))
                .uri("http://localhost:8082"))

        // Membership Service (port 8083)
        .route("membership-service-route",
            r -> r.path("/api/v1/memberships/**", "/api/v1/roles/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Route", "membership-service"))
                .uri("http://localhost:8083"))

        // Matrimony Service (port 8084)
        .route("matrimony-service-route",
            r -> r.path("/api/v1/matrimony/**", "/api/v1/profiles/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Route", "matrimony-service"))
                .uri("http://localhost:8084"))

        // Event Service (port 8085)
        .route("event-service-route",
            r -> r.path("/api/v1/events/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Route", "event-service"))
                .uri("http://localhost:8085"))

        // Analytics Service (port 8086)
        .route("analytics-service-route",
            r -> r.path("/api/v1/analytics/**", "/api/v1/reports/**", "/api/v1/dashboard/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Route", "analytics-service"))
                .uri("http://localhost:8086"))

        // Health checks (no routing, return 200 OK)
        .route("health-route",
            r -> r.path("/health", "/actuator/health")
                .uri("http://localhost:8080"))

        .build();
  }

  /**
   * Rate limiting: Use remote IP address as the key (limit per IP).
   * In production, could use authenticated user ID or API key instead.
   */
  @Bean
  public KeyResolver ipKeyResolver() {
    return exchange -> Mono.just(
        exchange.getRequest().getRemoteAddress() != null ?
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() :
            "anonymous"
    );
  }
}
