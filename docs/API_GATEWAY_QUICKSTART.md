# API Gateway Quick Start

## 🚀 Start Everything (5 minutes)

### Terminal 1: Start API Gateway (port 8080)
```powershell
cd e:\Himanshu\Workspace\AgrawalPulse\backend
mvn -pl api-gateway spring-boot:run -Dspring-boot.run.profiles=local
```

Wait for:
```
Started ApiGatewayApplication in 5.2 seconds
Netty started on port(s): 8080
```

### Terminal 2: Start User Service (port 8081)
```powershell
cd backend
mvn -pl user-service spring-boot:run -Dspring-boot.run.profiles=local
```

### Terminal 3: Start Family Service (port 8082)
```powershell
mvn -pl family-service spring-boot:run -Dspring-boot.run.profiles=local
```

### Terminal 4-5: Start Membership & Other Services (ports 8083-8086)
```powershell
mvn -pl membership-service spring-boot:run -Dspring-boot.run.profiles=local
mvn -pl matrimony-service spring-boot:run -Dspring-boot.run.profiles=local
mvn -pl event-service spring-boot:run -Dspring-boot.run.profiles=local
mvn -pl analytics-service spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 🧪 Test Routing

### Check Gateway Is Up
```powershell
curl http://localhost:8080/health
# Response: 200 OK {"status":"UP"}
```

### View All Routes
```powershell
curl http://localhost:8080/actuator/gatewayroutes | ConvertFrom-Json | ForEach-Object { Write-Host "$($_.route_id) -> $($_.uri)" }
```

Expected output:
```
user-service-route -> http://localhost:8081
family-service-route -> http://localhost:8082
membership-service-route -> http://localhost:8083
matrimony-service-route -> http://localhost:8084
event-service-route -> http://localhost:8085
analytics-service-route -> http://localhost:8086
health-route -> http://localhost:8080
```

### Route a Request Through Gateway
```powershell
# Request through GATEWAY (port 8080, not service directly)
curl -X GET "http://localhost:8080/api/v1/users" `
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"

# Gateway routes this to:
# http://localhost:8081/api/v1/users
# Response comes back through gateway
```

### Watch Correlation IDs
```powershell
# Make request through gateway
curl -v http://localhost:8080/actuator/health 2>&1 | grep -i "x-correlation"

# Response includes correlation ID header
# X-Correlation-ID: req-xyz-123
# X-Trace-ID: trace-abc-456
```

---

## 📊 Monitor Gateway

### Circuit Breaker Status
```powershell
curl http://localhost:8080/actuator/circuitbreakers | ConvertFrom-Json | ForEach-Object { Write-Host "$($_.name): $($_.state)" }
```

### Gateway Health
```powershell
curl http://localhost:8080/actuator/health
# Shows status of gateway + all downstream services
```

### Metrics
```powershell
curl http://localhost:8080/actuator/metrics | ConvertFrom-Json
# Shows request counts, latencies, error rates
```

---

## 🧪 Simulate Service Failure

### Make Gateway Fail Over

**Terminal 1**: Stop a service (e.g., family-service, Ctrl+C)

**Terminal 2**: Make requests to that service through gateway
```powershell
for ($i = 1; $i -le 15; $i++) {
  curl http://localhost:8080/api/v1/families 2>/dev/null
  Write-Host "Request $i"
}
```

**Watch**: 
- First 10 requests: Connection refused (service down)
- Request 11+: Circuit breaker OPEN, returns 503 immediately

**Verify**:
```powershell
curl http://localhost:8080/actuator/circuitbreakers | ConvertFrom-Json | Where-Object { $_.name -eq "default-circuit-breaker" }
# Output shows: "state": "OPEN"
```

---

## 📝 How It Works

```
1. Client Request (port 8080):
   GET /api/v1/families
   Authorization: Bearer token
   
2. Gateway RequestLoggingGatewayFilterFactory:
   - Extracts/generates X-Correlation-ID
   - Logs: "GATEWAY -> GET /api/v1/families"
   
3. Gateway Routes Request:
   - Path /api/v1/families/** matches family-service-route
   - Adds headers: X-Gateway-Route: family-service
   - Forwards to: http://localhost:8082/api/v1/families
   
4. Family Service Processes:
   - Receives request with correlation ID in headers
   - RequestLoggingFilter adds to MDC
   - Logs with correlationId baked in
   - Returns 200 OK
   
5. Gateway Logs Response:
   - "GATEWAY <- GET /api/v1/families - 45 ms"
   - Echoes correlation ID back to client
   
6. Client Receives:
   - 200 OK
   - X-Correlation-ID: same as request
   - Can trace this ID through all service logs
```

---

## 🔗 Architecture After Gateway

```
                      CLIENTS
                        ↓
                   API GATEWAY (port 8080)
                   [Single entry point]
            ┌──────┬──────┬──────┬──────┬──────┐
            ↓      ↓      ↓      ↓      ↓      ↓
          USER  FAMILY MEMBER MATRIMONY EVENT ANALYTICS
          8081  8082   8083    8084    8085   8086
         ┌──────────────────────────────────────┐
         │ Each service has:                    │
         │ - RequestLoggingFilter (correlation) │
         │ - JWT validation (security)          │
         │ - Resilience4j patterns              │
         │ - ELK logging integration            │
         └──────────────────────────────────────┘
```

---

## ✅ Success Criteria

All of these should work:

- [ ] `curl http://localhost:8080/health` returns 200 OK
- [ ] `curl http://localhost:8080/actuator/gatewayroutes` shows 6 service routes
- [ ] `curl http://localhost:8080/api/v1/users` routes to user-service (with valid JWT)
- [ ] `curl http://localhost:8080/api/v1/families` routes to family-service
- [ ] Stop a service → circuit breaker opens → next requests fail fast (503)
- [ ] Start service again → circuit breaker half-opens → traffic resumes
- [ ] Correlation IDs propagate through all services (check logs)

---

## 🚨 Troubleshooting

| Problem | Solution |
|---------|----------|
| Gateway won't start | Check port 8080 is free: `netstat -ano \| findstr :8080` |
| 404 on /api/v1/users | Ensure user-service is running on 8081 |
| 503 Service Unavailable | Circuit breaker is OPEN - service crashed. Restart service. |
| No correlation ID in logs | Check RequestLoggingFilter in common module is loaded |
| Timeout errors | Increase timeout in application-local.yml: `response-timeout: 30s` |

---

## 📖 Learn More

- Full API Gateway Guide: `docs/API_GATEWAY_GUIDE.md`
- Production Configuration: See "Production Considerations" section
- Resilience4j Integration: `docs/RESILIENCE4J_IMPLEMENTATION.md`
