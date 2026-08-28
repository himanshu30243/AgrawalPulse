# API Gateway Implementation Guide

## Overview

**API Gateway** is the single entry point for all client requests. It routes traffic to 6 independent microservices, providing cross-cutting concerns like authentication, rate limiting, and request tracking.

```
BEFORE (No Gateway):
  Client → Service A (8081)
  Client → Service B (8082)
  Client → Service C (8083)
  [Clients must know all service URLs]
  [No centralized auth/logging]

AFTER (With Gateway):
  Client → API Gateway (8080)
           ├→ Service A (8081)
           ├→ Service B (8082)
           ├→ Service C (8083)
           ├→ Service D (8084)
           ├→ Service E (8085)
           └→ Service F (8086)
  [Single entry point]
  [Centralized auth/logging/rate-limiting]
```

---

## Architecture

### What the Gateway Does

1. **Request Routing**
   - `/api/v1/users/**` → user-service:8081
   - `/api/v1/families/**` → family-service:8082
   - `/api/v1/memberships/**` → membership-service:8083
   - `/api/v1/matrimony/**` → matrimony-service:8084
   - `/api/v1/events/**` → event-service:8085
   - `/api/v1/analytics/**` → analytics-service:8086

2. **Cross-Cutting Concerns**
   - **JWT Authentication**: Validates tokens (from common module)
   - **CORS Handling**: Allows cross-origin requests
   - **Correlation ID**: Propagates X-Correlation-ID through all requests
   - **Request Logging**: Logs all requests/responses with timing
   - **Circuit Breaker**: Prevents cascade failures if service is down
   - **Rate Limiting**: Per-IP or per-user request throttling (future)

3. **Transparency**
   - Gateway adds headers but doesn't modify request/response bodies
   - Services receive requests as if client called them directly
   - Correlation IDs enable tracing through all layers

### Technology Stack

| Component | Purpose | Version |
|-----------|---------|---------|
| Spring Cloud Gateway | Route & filter requests | 4.1.2 |
| Resilience4j | Circuit breaker at gateway | 2.1.0 |
| Spring Cloud Sleuth | Automatic trace ID injection | 3.1.9 |
| Spring Boot | Web framework | 3.3.4 |

---

## Running the Gateway

### Start the Gateway

```powershell
cd backend
mvn -pl api-gateway spring-boot:run -Dspring-boot.run.profiles=local
```

**Expected output:**
```
Started ApiGatewayApplication in 5.2 seconds
Netty started on port(s): 8080
```

### Test Routing

```powershell
# Request through gateway (port 8080)
curl -X GET "http://localhost:8080/api/v1/users" \
  -H "Authorization: Bearer <your-jwt-token>"

# This gets routed to user-service:8081
# Gateway transparently forwards the request
# Response: 200 OK with user data
```

### Verify Routes

```powershell
# List all configured routes
curl http://localhost:8080/actuator/gatewayroutes

# Expected response:
# [
#   {
#     "route_id": "user-service-route",
#     "uri": "http://localhost:8081",
#     "order": 0
#   },
#   ...
# ]
```

---

## Request Flow Example

```
1. Client sends:
   POST /api/v1/families
   X-Correlation-ID: req-xyz-123
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   
2. Gateway receives request
   - Extracts/validates JWT (via common module SecurityConfig)
   - Checks correlation ID (adds if missing)
   - Logs: "GATEWAY -> POST /api/v1/families [correlationId=req-xyz-123]"
   
3. Gateway routes to family-service:8082
   - RequestLoggingGatewayFilterFactory adds headers:
     * X-Correlation-ID: req-xyz-123
     * X-Trace-ID: trace-abc-456
   - Passes through with Authorization header intact
   
4. family-service receives:
   - JWT validated again by local security config
   - Creates family, logs with correlationId from MDC
   - Returns 201 Created
   
5. Gateway logs response:
   - "GATEWAY <- POST /api/v1/families - 45 ms [status=201]"
   
6. Client receives:
   - 201 Created with location header
   - X-Correlation-ID: req-xyz-123 (echoed back)
   - X-Trace-ID: trace-abc-456 (echoed back)
   
7. Tracing:
   - grep "req-xyz-123" in all service logs
   - See complete request path: client → gateway → family-service
```

---

## Configuration

### Local Configuration (application-local.yml)

```yaml
spring:
  cloud:
    gateway:
      # Default filters applied to all routes
      default-filters:
        - name: RequestLoggingGatewayFilterFactory
        - name: CircuitBreaker
      # CORS configuration
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods: GET, POST, PUT, DELETE, PATCH
            allowedHeaders: "*"
            maxAge: 3600

server:
  port: 8080
```

### Route Configuration (in GatewayConfig.java)

```java
// Example: Add a new route
.route("new-service",
  r -> r.path("/api/v1/new-endpoint/**")
    .filters(f -> f
      .stripPrefix(0)
      .addRequestHeader("X-Gateway-Route", "new-service"))
    .uri("http://localhost:8087"))
```

### Rate Limiting (Future Enhancement)

```yaml
resilience4j:
  ratelimiter:
    instances:
      default:
        limitForPeriod: 100
        limitRefreshPeriod: 1m
        timeoutDuration: 5s
```

---

## Circuit Breaker at Gateway Level

If a service is down or responding slowly:

### Scenario: family-service Crashes

```
Request 1-10: Normal routing
  → family-service returns errors
  → Gateway tracks error rate (50% threshold)

Request 11+: Circuit breaker opens
  → Gateway returns 503 Service Unavailable immediately
  → Does NOT attempt to route to family-service
  → Client fails fast (not hung for 30 seconds)

After 30 seconds: Circuit breaker half-open
  → Allows 3 test requests through
  → If success: circuit closes, traffic resumes
  → If fail: circuit opens again
```

**Check status:**
```powershell
curl http://localhost:8080/actuator/circuitbreakers
```

**Response:**
```json
{
  "circuitbreakers": [
    {
      "name": "default-circuit-breaker",
      "state": "OPEN",
      "details": {
        "failureRate": 100.0,
        "slowCallRate": 0.0
      }
    }
  ]
}
```

---

## Custom Gateway Filters

### RequestLoggingGatewayFilterFactory

Automatically logs all requests/responses with correlation IDs:

```
2026-08-28 15:40:23.456  INFO (...) - GATEWAY -> POST /api/v1/families [correlationId=req-xyz-123]
2026-08-28 15:40:23.501  INFO (...) - GATEWAY <- POST /api/v1/families - 45 ms [status=201]
```

**Location**: `backend/api-gateway/src/main/java/com/agrawalpulse/gateway/RequestLoggingGatewayFilterFactory.java`

**Features**:
- Extracts X-Correlation-ID from request headers
- Generates if missing (UUID-based)
- Adds to response headers (so clients can trace their request)
- Propagates to downstream services
- Logs timing information

---

## Production Considerations

### 1. Service Discovery (Eureka)

Current setup uses hardcoded URLs (localhost:8081, etc).

For production with multiple instances:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service  # Load balancer with Eureka
          predicates:
            - Path=/api/v1/users/**
```

Requires: Spring Cloud Eureka client in gateway + each service

### 2. Load Balancing

Gateway itself should run behind a load balancer:

```
                    ┌─────────────────┐
                    │ Load Balancer   │
                    │ (NGINX/HAProxy) │
                    └────────┬────────┘
                   ┌──────┬──┴────┬──────┐
                   ↓      ↓       ↓      ↓
              Gateway1 Gateway2 Gateway3...
                   │      │       │      │
                   └──────┼───────┴──────┘
                          ↓
                   Microservices (8081-8086)
```

### 3. SSL/TLS

```yaml
server:
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
    key-store-type: PKCS12
  port: 8443
```

### 4. Authentication at Gateway

Currently validates JWT in each service (common module SecurityConfig).

For centralized auth:

```java
@Component
public class AuthenticationGatewayFilter extends AbstractGatewayFilterFactory<Config> {
  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      // Validate JWT here
      // Extract user context
      // Pass to downstream services
      return chain.filter(exchange);
    };
  }
}
```

### 5. Rate Limiting by User

```yaml
resilience4j:
  ratelimiter:
    instances:
      per-user:
        limitForPeriod: 1000  # 1000 req/min per user
        limitRefreshPeriod: 1m
```

Then use `userKeyResolver()` instead of `ipKeyResolver()`:

```java
@Bean
public KeyResolver userKeyResolver() {
  return exchange -> Mono.just(
      getPrincipalName(exchange)  // authenticated user ID
  );
}
```

---

## Monitoring & Metrics

### Actuator Endpoints

```
http://localhost:8080/actuator/health         # Gateway health
http://localhost:8080/actuator/gatewayroutes   # All routes
http://localhost:8080/actuator/circuitbreakers # Circuit breaker state
http://localhost:8080/actuator/metrics         # Metrics data
```

### Key Metrics to Track

```
gateway_requests_seconds_count         # Total requests
gateway_requests_seconds_max           # Max request time
spring_cloud_gateway_filters_requests  # Filter execution time
resilience4j_circuitbreaker_state      # Circuit breaker state
```

### Example Grafana Dashboard

```
Graph 1: Request Rate (req/sec)
  Query: rate(gateway_requests_seconds_count[1m])

Graph 2: P95 Latency
  Query: histogram_quantile(0.95, gateway_requests_seconds_bucket)

Graph 3: Circuit Breaker Status
  Query: resilience4j_circuitbreaker_state{name=~".*"}
  
Graph 4: Error Rate
  Query: rate(gateway_requests_seconds_count{outcome="SERVER_ERROR"}[1m])
```

---

## Troubleshooting

### Gateway Starts But Routes Don't Work

**Symptom**: 404 errors even though path matches

**Solutions**:
1. Check route order - more specific routes first
2. Verify `stripPrefix(0)` - don't strip path for `/api/v1/**` routes
3. Ensure service is running on correct port

```powershell
# Debug: Print all routes
curl http://localhost:8080/actuator/gatewayroutes | ConvertFrom-Json | ForEach-Object { $_.route_id, $_.uri }
```

### Requests Timeout

**Symptom**: 503 or timeout after 60 seconds

**Solutions**:
1. Check if target service is running
2. Check circuit breaker status (may be OPEN)
3. Increase timeout (if service is legitimately slow):

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 5000
        response-timeout: 30s
```

### Correlation ID Not Propagating

**Symptom**: X-Correlation-ID missing in downstream logs

**Solutions**:
1. Check RequestLoggingGatewayFilterFactory is loaded
2. Verify RequestLoggingFilter in each service's common module
3. Ensure spring-cloud-sleuth is in pom.xml

```powershell
# Verify filter is active
curl -v http://localhost:8080/api/v1/users 2>&1 | grep -i "x-correlation"
```

---

## Files Overview

| File | Purpose |
|------|---------|
| `pom.xml` | Dependencies (Spring Cloud Gateway, Resilience4j) |
| `ApiGatewayApplication.java` | Main entry point |
| `GatewayConfig.java` | Route definitions & CORS config |
| `RequestLoggingGatewayFilterFactory.java` | Custom filter for logging & correlation IDs |
| `application.yml` | Gateway configuration (ports, filters) |
| `application-local.yml` | Local dev profile (hardcoded URLs) |

---

## Next Steps

1. **Test locally**
   - Start all 6 services on ports 8081-8086
   - Start gateway on port 8080
   - Make requests through gateway, verify routing

2. **Monitor**
   - Check `/actuator/gatewayroutes`
   - Watch correlation IDs in logs
   - Monitor circuit breaker state

3. **Production Readiness**
   - Implement Eureka service discovery
   - Set up load balancer in front of gateway
   - Configure SSL/TLS
   - Implement centralized authentication
   - Add rate limiting by user/API key

---

## References

- Spring Cloud Gateway: https://spring.io/projects/spring-cloud-gateway
- Resilience4j Circuit Breaker: https://resilience4j.readme.io/
- Gateway Routing: https://cloud.spring.io/spring-cloud-gateway/reference/html/#gateway-routes-configuration
