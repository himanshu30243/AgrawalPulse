# Resilience4j Implementation Guide

## Critical Gap Addressed ✅

**Problem**: Your microservices had zero fault tolerance:
- ❌ No timeouts on any inter-service REST calls
- ❌ No circuit breaker (cascade failures when 1 service fails)
- ❌ No retry logic (transient failures = permanent failure)
- ❌ No bulkhead pattern (no thread pool isolation)

**Impact if one service fails**:
```
User → API Gateway → Service A
                       ↓ [TIMEOUT - no timeout config, waits forever]
                       ↓ Service B (30 second wait...)
                       ↓ Service C (30 second wait...) 
                       ↓ Service D (30 second wait...)
Result: All services blocked, cascading failure
```

**Solution Implemented**: Resilience4j with circuit breaker, retry, and timeout patterns.

---

## What Was Added

### 1. **Dependencies** (common/pom.xml)
```
resilience4j-spring-boot3 v2.1.0 (main library)
resilience4j-circuitbreaker (opens on failures)
resilience4j-retry (2 attempts on transient errors)
resilience4j-timelimiter (5s timeout, 3s for user-service)
resilience4j-bulkhead (thread pool isolation - future)
resilience4j-micrometer (metrics & monitoring)
```

### 2. **Configuration Classes** (common module)
#### `ResilienceConfig.java`
- Defines circuit breaker strategies per service
- Configures retry policies (max 2 attempts)
- Sets time limiters (5s default, 3s for user-service)
- Creates Spring beans for each service

**Circuit Breaker Thresholds**:
```
Default (most services):
  - Opens when: 50% error rate over 10 calls
  - Waits: 30 seconds before retrying
  - Retries: 3 calls in half-open state

user-service (stricter):
  - Opens when: 40% error rate over 15 calls
  - Waits: 20 seconds before retrying
  - Reason: Most critical service, needs faster recovery
```

#### `ResilienceInterceptor.java`
- Intercepts all RestClient calls
- Applies circuit breaker → retry pattern automatically
- Logs failures and state changes
- No code changes needed in services

### 3. **YAML Configuration** (all 6 services)
Each service's `application-local.yml` now has:

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        
  retry:
    configs:
      default:
        maxAttempts: 2
        waitDuration: 100ms
        
  timelimiter:
    configs:
      default:
        timeoutDuration: 5000ms
```

### 4. **Actuator Endpoints** (all services)
Exposed for monitoring circuit breaker status:
```
http://localhost:8081/actuator/circuitbreakers
http://localhost:8081/actuator/retries
http://localhost:8081/actuator/timelimiters
http://localhost:8081/actuator/health
```

---

## How It Works

### Scenario 1: Normal Call
```
Request to user-service
  → CircuitBreaker [CLOSED] ✓
  → Retry [Ready] ✓
  → Execute HTTP call
  → Response 200 ✓
```

### Scenario 2: Transient Failure (network blip)
```
Request to membership-service
  → CircuitBreaker [CLOSED] ✓
  → Retry [Active] - Attempt 1
  → Connection timeout ✗
  → Wait 100ms
  → Retry [Active] - Attempt 2
  → Success 200 ✓
```

### Scenario 3: Service Down (Cascading Failure Prevention)
```
analytics-service crashes. First 10 calls fail:
  → CircuitBreaker [CLOSED] ✓
  → Call 1-10: All fail (50% threshold reached)
  → CircuitBreaker now [OPEN] ⛔
  
Next request:
  → CircuitBreaker [OPEN] ⛔ - Short-circuit, don't call
  → Immediately throw: "Service temporarily unavailable (circuit breaker open)"
  → Client gets fast failure instead of hanging 30 seconds
  
After 30 seconds:
  → CircuitBreaker [HALF-OPEN] ⚠️ - Allow 3 test calls
  → If success: [CLOSED] ✓ (traffic resumes)
  → If fail: [OPEN] ⛔ (wait another 30 seconds)
```

---

## Files Changed

| File | Changes |
|------|---------|
| `backend/common/pom.xml` | Added 6 resilience4j dependencies |
| `backend/common/src/main/java/com/agrawalpulse/resilience/ResilienceConfig.java` | NEW - Configuration beans |
| `backend/common/src/main/java/com/agrawalpulse/resilience/ResilienceInterceptor.java` | NEW - HTTP interceptor |
| `backend/*/application-local.yml` (all 6 services) | Added resilience4j config + actuator endpoints |
| `backend/src/main/resources/application-resilience.yml` | Reference config file |

---

## Testing the Implementation

### Test 1: Verify Circuit Breaker Beans Loaded
```powershell
# Start family-service
cd backend
mvn -pl family-service spring-boot:run -Dspring-boot.run.profiles=local

# In another terminal, check health
curl http://localhost:8082/actuator/health
```

Expected output:
```json
{
  "status": "UP",
  "components": {
    "circuitbreakers": { "status": "UP" },
    "resilience4j": { "status": "UP" }
  }
}
```

### Test 2: View Circuit Breaker Status
```powershell
curl http://localhost:8082/actuator/circuitbreakers
```

Expected output:
```json
{
  "circuitbreakers": [
    {
      "name": "user-service",
      "state": "CLOSED",
      "details": {
        "bufferSize": 15,
        "failureRate": 0.0,
        "lastErrorMessage": null
      }
    }
  ]
}
```

### Test 3: Simulate Service Failure
1. Stop analytics-service
2. Make 12 calls to an endpoint that calls analytics-service
3. Watch circuit breaker transition: CLOSED → OPEN
4. Subsequent calls fail immediately (not timeout)

```powershell
# Stop analytics-service (Ctrl+C in its terminal)

# Call endpoint 12 times
for ($i = 1; $i -le 12; $i++) {
  curl -X GET "http://localhost:8082/api/v1/families"
  Write-Host "Call $i"
}

# Check circuit breaker status
curl http://localhost:8082/actuator/circuitbreakers
# Should show state: OPEN
```

---

## Production Configuration Guidance

### For High-Traffic Services
```yaml
resilience4j:
  circuitbreaker:
    instances:
      user-service:
        slidingWindowSize: 100  # More calls before deciding
        failureRateThreshold: 40  # Stricter threshold
        waitDurationInOpenState: 60s  # Give service longer to recover
```

### For Low-Latency Requirements
```yaml
resilience4j:
  timelimiter:
    instances:
      user-service:
        timeoutDuration: 2000ms  # Faster fail instead of hanging
```

### Metrics/Monitoring Setup
```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
  endpoints:
    web:
      exposure:
        include: health,info,circuitbreakers,retries,metrics,prometheus
```

Then query Prometheus:
```
resilience4j_circuitbreaker_calls_total{kind="successful"}
resilience4j_circuitbreaker_state{name="user-service",state="OPEN"}
resilience4j_retry_attempts_total{retry="user-service"}
```

---

## Integration with Existing Code

✅ **Zero Changes Required to Services**

The `ResilienceInterceptor` is automatically applied to all RestClient calls via Spring's interceptor mechanism. No code changes needed:

```java
// Your existing code - still works!
ClientHttpResponse response = restClient
    .get()
    .uri("http://user-service:8081/api/v1/users")
    .retrieve()
    .toEntity(UserDto.class);

// Under the hood now:
// 1. ResilienceInterceptor intercepts the call
// 2. Extracts service name: "user-service"
// 3. Gets circuit breaker for "user-service"
// 4. Gets retry policy for "user-service"
// 5. Executes with fault tolerance
```

---

## Metrics Available

All services expose these actuator endpoints:

### Circuit Breaker Status
```
GET /actuator/circuitbreakers
```
Shows state (CLOSED/OPEN/HALF_OPEN), failure rate, buffer size

### Retry Status
```
GET /actuator/retries
```
Shows retry attempts, success/failure counts

### Health Combined
```
GET /actuator/health
```
Aggregated status of all resilience patterns

### Prometheus Metrics (when enabled)
```
resilience4j_circuitbreaker_calls_total
resilience4j_circuitbreaker_state
resilience4j_retry_attempts_total
resilience4j_retry_calls_total
```

---

## Next Steps

1. **Test locally** - Verify circuit breaker opens when a service crashes
2. **Monitor in staging** - Watch metrics dashboard as traffic flows
3. **Tune thresholds** - Adjust failure rates / timeout durations per service
4. **Production** - Apply with confidence, cascade failures are prevented

---

## Reference

- Resilience4j Docs: https://resilience4j.readme.io/
- Circuit Breaker Pattern: https://martinfowler.com/bliki/CircuitBreaker.html
- Spring Boot Integration: https://github.com/resilience4j/resilience4j/tree/master/resilience4j-spring-boot3
