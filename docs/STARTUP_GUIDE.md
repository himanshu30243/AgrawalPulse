# AgrawalPulse Application Startup Guide

## ⚠️ Important Note

**Jenkins is running on port 8080**, so the API Gateway has been configured to run on **port 8090** instead.

## Prerequisites

- ✅ Maven 3.9.12 installed
- ✅ PostgreSQL running on localhost:5432
- ✅ All 11 modules compiled successfully

## Service Ports

| Service | Port | Purpose |
|---------|------|---------|
| Eureka Server | 8761 | Service registry & discovery |
| Config Server | 8888 | Centralized configuration |
| **API Gateway** | **8090** | Single entry point (NOT 8080 - Jenkins uses that) |
| User Service | 8081 | User management |
| Family Service | 8082 | Family data |
| Membership Service | 8083 | Membership management |
| Matrimony Service | 8084 | Matrimony profiles |
| Event Service | 8085 | Event management |
| Analytics Service | 8086 | Analytics & reporting |

---

## Startup Order

Start services in **9 separate terminals** in this exact order:

### Terminal 1: Eureka Server (port 8761)
```powershell
cd e:\Himanshu\Workspace\AgrawalPulse\backend
mvn -pl eureka-server spring-boot:run
```

**Wait for output:**
```
Started EurekaServerApplication in X seconds
Tomcat started on port(s): 8761
```

### Terminal 2: Config Server (port 8888)
```powershell
cd e:\Himanshu\Workspace\AgrawalPulse\backend
mvn -pl config-server spring-boot:run
```

**Wait for output:**
```
Started ConfigServerApplication in X seconds
Tomcat started on port(s): 8888
```

### Terminal 3: API Gateway (port 8090) ⭐ NOTE: 8090, NOT 8080
```powershell
cd e:\Himanshu\Workspace\AgrawalPulse\backend
mvn -pl api-gateway spring-boot:run -Dspring-boot.run.profiles=local
```

**Wait for output:**
```
Started ApiGatewayApplication in X seconds
Netty started on port(s): 8090
```

### Terminal 4: User Service (port 8081)
```powershell
cd e:\Himanshu\Workspace\AgrawalPulse\backend
mvn -pl user-service spring-boot:run -Dspring-boot.run.profiles=local
```

### Terminal 5: Family Service (port 8082)
```powershell
cd e:\Himanshu\Workspace\AgrawalPulse\backend
mvn -pl family-service spring-boot:run -Dspring-boot.run.profiles=local
```

### Terminal 6: Membership Service (port 8083)
```powershell
cd e:\Himanshu\Workspace\AgrawalPulse\backend
mvn -pl membership-service spring-boot:run -Dspring-boot.run.profiles=local
```

### Terminal 7: Matrimony Service (port 8084)
```powershell
cd e:\Himanshu\Workspace\AgrawalPulse\backend
mvn -pl matrimony-service spring-boot:run -Dspring-boot.run.profiles=local
```

### Terminal 8: Event Service (port 8085)
```powershell
cd e:\Himanshu\Workspace\AgrawalPulse\backend
mvn -pl event-service spring-boot:run -Dspring-boot.run.profiles=local
```

### Terminal 9: Analytics Service (port 8086)
```powershell
cd e:\Himanshu\Workspace\AgrawalPulse\backend
mvn -pl analytics-service spring-boot:run -Dspring-boot.run.profiles=local
```

---

## Verify Application is Running

### Check Eureka Dashboard
```powershell
# Open browser: http://localhost:8761
# You should see all 8 services registered (gateway + 7 services)
```

### Check Service Registration
```powershell
curl http://localhost:8761/eureka/apps
# Returns XML with all registered services
```

### Check API Gateway Health
```powershell
curl http://localhost:8090/health
# Should return: {"status":"UP"}
```

### Check Individual Service Health
```powershell
curl http://localhost:8081/actuator/health  # user-service
curl http://localhost:8082/actuator/health  # family-service
curl http://localhost:8083/actuator/health  # membership-service
# etc.
```

### Check Eureka Routes
```powershell
curl http://localhost:8090/actuator/gatewayroutes
# Returns list of all configured routes
```

---

## Make API Requests Through Gateway

### Get JWT Token (Local Auth)
```powershell
curl -X POST http://localhost:8090/api/v1/local-auth/token `
  -H "Content-Type: application/json" `
  -d '{
    "email": "himanshu_admin@gmail.com",
    "password": "any-password",
    "branch": "global"
  }'

# Copy the returned token
```

### Use Token to Call API
```powershell
curl -X GET http://localhost:8090/api/v1/users `
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Request goes:
# Client → API Gateway (8090)
#          → User Service (8081, via Eureka load balancer)
#          → Returns response
```

### Example Requests
```powershell
# Get users
curl http://localhost:8090/api/v1/users -H "Authorization: Bearer TOKEN"

# Get families
curl http://localhost:8090/api/v1/families -H "Authorization: Bearer TOKEN"

# Get memberships
curl http://localhost:8090/api/v1/memberships -H "Authorization: Bearer TOKEN"

# Get analytics dashboard
curl http://localhost:8090/api/v1/dashboard -H "Authorization: Bearer TOKEN"
```

---

## What's Happening Under the Hood

```
1. Service Startup:
   Service A starts
   → Reads bootstrap.yml (Eureka location: 8761)
   → Registers with Eureka
   → Fetches config from Config Server (8888)
   → Initializes cache (Redis or in-memory)
   → Ready to accept requests

2. API Request:
   Client → API Gateway (8090)
   → Gateway queries Eureka: "Where is user-service?"
   → Eureka returns: localhost:8081
   → Gateway routes request to 8081
   → Service processes request with logged correlation ID
   → Response returns through gateway

3. Service Communication:
   User Service → needs Family data
   → Eureka discovery: "Where is family-service?"
   → Direct HTTP call to localhost:8082
   → Resilience4j handles timeouts/retries
   → Cache stores result (Redis)
```

---

## Troubleshooting

### "Port 8090 already in use"
- Jenkins is running on 8080 ✓ (expected)
- Check if another service is using 8090: `netstat -ano | findstr :8090`
- Kill the process if needed

### "Cannot connect to Eureka"
- Check if Eureka Server (8761) is running
- `curl http://localhost:8761` should work

### "Cannot connect to Config Server"
- Check if Config Server (8888) is running
- `curl http://localhost:8888` should work

### Service not registering with Eureka
- Check logs for "register-with-eureka"
- Verify bootstrap.yml exists in service's src/main/resources/
- Wait 30 seconds after service starts (Eureka refresh interval)

### 503 Service Unavailable
- Circuit breaker is OPEN (service crashed)
- Check target service's logs
- Wait 30 seconds for circuit breaker to half-open

### "Cannot connect to PostgreSQL"
- Verify PostgreSQL is running: `psql -U agrawalpulse -d agrawalpulse`
- Check connection strings in application-local.yml

---

## Port Reference Quick

| Service | Port | Quick URL |
|---------|------|-----------|
| Jenkins | 8080 | http://localhost:8080 |
| **API Gateway** | **8090** | http://localhost:8090 ⭐ |
| User Service | 8081 | http://localhost:8081/actuator/health |
| Family Service | 8082 | http://localhost:8082/actuator/health |
| Membership | 8083 | http://localhost:8083/actuator/health |
| Matrimony | 8084 | http://localhost:8084/actuator/health |
| Event | 8085 | http://localhost:8085/actuator/health |
| Analytics | 8086 | http://localhost:8086/actuator/health |
| Eureka | 8761 | http://localhost:8761 (Dashboard) |
| Config Server | 8888 | http://localhost:8888 |

---

## Next Steps

1. ✅ Verify PostgreSQL is running
2. ✅ Build all modules: `cd backend && mvn clean install -DskipTests=true`
3. ✅ Start services in 9 terminals (order matters!)
4. ✅ Check Eureka dashboard: http://localhost:8761
5. ✅ Make API requests through gateway: http://localhost:8090/api/v1/...
6. ✅ Monitor logs for errors

**Everything should be running in ~2-3 minutes!** 🚀
