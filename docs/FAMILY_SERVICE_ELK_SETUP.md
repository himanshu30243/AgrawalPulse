# Family Service ELK Setup - Quick Start

## Prerequisites Completed ✅

- ✅ Maven dependencies added to `common/pom.xml` (logstash-logback-encoder, spring-cloud-sleuth)
- ✅ `logback-spring.xml` created with JSON logging config
- ✅ `LogContext.java` utility class added to common module
- ✅ `RequestLoggingFilter.java` filter added to common module  
- ✅ Backend compiled successfully (`mvn clean install`)
- ✅ `application-local.yml` updated with Sleuth config

## What's Ready

1. **JSON Logging**: All logs from family-service will be JSON-structured with MDC context
2. **Correlation IDs**: Every request gets a unique X-Correlation-ID header
3. **Trace IDs**: Spring Cloud Sleuth auto-adds traceId/spanId to all logs
4. **Local File Output**: Logs written to `logs/family-service-json.log` and `logs/family-service-text.log`

## Next Steps (Follow in your terminal)

### Step 1: Install Docker (One-time setup)

Download Docker Desktop from https://www.docker.com/products/docker-desktop

After installation, verify:
```powershell
docker --version
docker compose version
```

### Step 2: Start ELK Stack

Open Terminal 1 and run:
```powershell
cd e:\Himanshu\Workspace\AgrawalPulse
docker compose -f docker-compose.elk.yml up
```

**Wait for all services to be healthy (~30 seconds):**
- Elasticsearch ✓ (port 9200)
- Logstash ✓ (port 5000)
- Filebeat ✓
- Kibana ✓ (port 5601)

### Step 3: Start Family Service

Open Terminal 2 and run:
```powershell
cd e:\Himanshu\Workspace\AgrawalPulse\backend
mvn -pl family-service spring-boot:run -Dspring-boot.run.profiles=local
```

**Wait for startup message:**
```
Started FamilyServiceApplication in X seconds
```

### Step 4: Generate Some Logs

Open Terminal 3 and make API calls:

```powershell
# Get all families (creates logs)
curl -X GET "http://localhost:8082/api/v1/families" `
  -H "Authorization: Bearer <your-jwt-token>"

# Or create a family (more interesting logs)
curl -X POST "http://localhost:8082/api/v1/families" `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer <your-jwt-token>" `
  -d '{
    "name": "Smith Family",
    "description": "Test family"
  }'
```

**Check logs were generated:**
```powershell
# See files created
Get-ChildItem "e:\Himanshu\Workspace\AgrawalPulse\logs\"

# View JSON logs
Get-Content "e:\Himanshu\Workspace\AgrawalPulse\logs\family-service-json.log" -Tail 20
```

### Step 5: View in Kibana

1. Open browser: **http://localhost:5601**
2. Go to **Management → Index Patterns**
3. Create index pattern: `agrawalpulse-*`
4. Select timestamp: `@timestamp`
5. Go to **Discover**
6. Search for logs from family-service

### Sample Kibana Queries (KQL)

```
# All family-service logs
service:family-service

# Only errors
service:family-service AND level:ERROR

# By correlation ID (trace across services)
mdc.correlationId:"req-xyz"

# Slow requests (>500ms)
service:family-service AND responseTimeMs:>500
```

## What You Should See

### In Terminal (Console Logs)
```
2026-08-28 14:30:45.123  INFO 12345 --- [http-nio-8082-exec-1] c.a.f.controller.FamilyController : HTTP GET /api/v1/families - 45 ms [200]
```

### In Logs Directory (JSON Logs)
File: `logs/family-service-json.log`
```json
{
  "@timestamp": "2026-08-28T14:30:45.123Z",
  "level": "INFO",
  "logger_name": "com.agrawalpulse.family.controller.FamilyController",
  "message": "HTTP GET /api/v1/families - 45 ms [200]",
  "thread_name": "http-nio-8082-exec-1",
  "mdc": {
    "traceId": "a4fb4a11ddea4e1d",
    "spanId": "a4fb4a11ddea4e1d",
    "correlationId": "req-789-xyz"
  },
  "service": "family-service",
  "environment": "local"
}
```

### In Kibana (Logs Tab)
- See JSON logs parsed and displayed
- Click fields to see correlationId, traceId, responseTimeMs
- Create dashboards showing latency trends

## Troubleshooting

### Elasticsearch health check fails
```bash
curl http://localhost:9200/_cluster/health
```

If DOWN, check:
- Docker container running: `docker ps`
- Logs: `docker logs elasticsearch` (or Logstash/Kibana)
- Disk space in Docker

### No logs appearing in Kibana
1. Verify JSON logs are being generated:
   ```powershell
   Get-Content "logs/family-service-json.log" -Tail 10
   ```
2. Check Filebeat is reading files:
   ```bash
   docker logs filebeat
   ```
3. Check Logstash pipeline:
   ```bash
   curl http://localhost:9600/_node/stats/pipelines
   ```
4. Verify index was created:
   ```bash
   curl http://localhost:9200/_cat/indices
   ```

## Architecture Review

```
family-service (port 8082)
  ↓
logs/family-service-json.log (local rotation: 7 days, 10MB per file)
  ↓
filebeat (watches logs/ directory)
  ↓
logstash (processes JSON, enriches data)
  ↓
elasticsearch (indexes: agrawalpulse-family-service-2026.08.28)
  ↓
kibana (http://localhost:5601 - visualize & search)
```

## Next: Rollout to Other Services

Once this works perfectly:

1. **User Service** (port 8081) - Copy logback config
2. **Membership Service** (port 8083) - Test cross-service tracing
3. **Matrimony, Event, Analytics** - Parallel deployment

All services share the same:
- `logback-spring.xml` (in backend/src/main/resources)
- `LogContext` + `RequestLoggingFilter` (from common module)
- ELK stack (same Elasticsearch instance)

Just update `application-local.yml` for each service with Sleuth config.

## Questions?

Check the ELK Implementation Guide at: `https://claude.ai/code/artifact/13fc7be3-1eb2-4843-a364-d4b11d6c1bf2`
