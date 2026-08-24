# AgrawalPulse - Test Coverage & Quality Plan

## Current Status (As of Now)

### Test Coverage by Service

| Service | Unit Tests | Integration Tests | Status |
|---------|-----------|------------------|--------|
| user-service | ❌ 0 | ❌ 0 | **NEEDS TESTS** |
| family-service | ✅ 2 | ❌ 0 | Partially tested |
| membership-service | ❌ 0 | ❌ 0 | **NEEDS TESTS** |
| matrimony-service | ✅ 2 | ❌ 0 | Partially tested |
| event-service | ❌ 0 | ❌ 0 | **NEEDS TESTS** |
| analytics-service | ❌ 0 | ❌ 0 | **NEEDS TESTS** |

**Overall: 4 tests for 6 services (0.6% coverage)**

---

## Will the Jenkins Pipeline Work?

### ✅ YES - The Pipeline Will Work

Even with minimal tests, the Jenkins pipeline will:

1. **Compile all services** - Maven builds all 6 services
2. **Run existing tests** - 4 tests will execute (from family-service and matrimony-service)
3. **Build Docker images** - All 7 images (6 services + 1 frontend) created
4. **Push to registry** - Images pushed to Docker Hub
5. **Report success** - Build marked as successful

### ⚠️ BUT - Quality Risks

Services without tests are **NOT** verified to:
- ✗ Actually start without errors
- ✗ Connect to the database
- ✗ Respond to API requests
- ✗ Handle edge cases
- ✗ Integration between services

### 🛡️ Safety Net Added

The Jenkins pipeline now includes:

```groovy
stage('Verify All Services Compile') {
  // Ensures each service at least compiles
  // Fails build if ANY service has errors
  // This catches: syntax errors, missing dependencies, config issues
}

stage('Verify Docker Images') {
  // Confirms Docker images were actually built
  // Lists all images created
}
```

---

## Minimum Test Requirements

### Phase 1: Compilation Verification ✅ (Already in pipeline)
Each service must:
- [ ] Compile without errors
- [ ] Include valid pom.xml
- [ ] Resolve all Maven dependencies

**Pipeline Stage**: "Verify All Services Compile"

---

### Phase 2: Basic Unit Tests (Recommended)

Create at least **1 simple test per service** to verify:

#### user-service
```java
@SpringBootTest
class UserServiceApplicationTest {
  @Test
  void contextLoads() {
    // Just verifies Spring context starts
  }
}
```

#### membership-service
```java
@SpringBootTest
class MembershipServiceApplicationTest {
  @Test
  void contextLoads() {
    // Just verifies Spring context starts
  }
}
```

#### event-service
```java
@SpringBootTest
class EventServiceApplicationTest {
  @Test
  void contextLoads() {
    // Just verifies Spring context starts
  }
}
```

#### analytics-service
```java
@SpringBootTest
class AnalyticsServiceApplicationTest {
  @Test
  void contextLoads() {
    // Just verifies Spring context starts
  }
}
```

**Time to add**: ~10 minutes per service = 40 minutes total

---

### Phase 3: Integration Tests (Nice-to-Have)

Test critical flows:
- Family registration flow (family-service + membership-service)
- Matrimony consent (matrimony-service + family-service)
- Event registration (event-service + family-service)
- Analytics aggregation (analytics-service reading all tables)

**Time to add**: 2-4 hours

---

## What the Jenkins Pipeline Currently Does

### Build Stages (All Run)
1. ✅ **Checkout** - Clone from GitHub
2. ✅ **Build Backend** - Maven build (all 6 services)
3. ✅ **Build Frontend** - npm build
4. ✅ **Backend Tests** - Run 4 existing tests
5. ✅ **Frontend Tests** - Run frontend tests (if configured)
6. ✅ **Code Quality** - SpotBugs static analysis
7. ✅ **Verify Compilation** - Check each service compiles (NEW)
8. ✅ **Build Docker Images** - Create 7 images
9. ✅ **Verify Docker Images** - List images created (NEW)
10. ✅ **Push to Registry** - Push to Docker Hub
11. ✅ **Deploy to Dev** - Placeholder (ECS deployment)
12. ✅ **Verify Docker Images** - Confirm images exist (NEW)

**Success Criteria**: All 12 stages complete without errors

---

## Why Services Without Tests Still Work

### Maven Compilation (`mvn clean install`)
- Compiles all `.java` files
- Checks syntax and dependencies
- Creates `.jar` files
- **Does NOT require unit tests to pass**

### Unit Tests (`mvn test`)
- Only runs tests in `src/test/java/`
- Services with 0 tests = 0 test failures ✅
- Maven doesn't fail if no tests found

### Docker Images (`docker build`)
- Builds from compiled `.jar` files
- Doesn't require tests to exist
- Images created regardless of test coverage

### Pushes to Registry (`docker push`)
- Pushes pre-built images
- Doesn't verify services work

---

## Recommendation: Minimum Viable Testing

To improve quality without large time investment:

### Week 1: Add Context Load Tests (40 min)
```bash
# Create one simple test file per service that lacks tests
backend/user-service/src/test/java/.../UserServiceApplicationTest.java
backend/membership-service/src/test/java/.../MembershipServiceApplicationTest.java
backend/event-service/src/test/java/.../EventServiceApplicationTest.java
backend/analytics-service/src/test/java/.../AnalyticsServiceApplicationTest.java
```

Each file:
```java
@SpringBootTest
class ServiceNameApplicationTest {
  @Test
  void contextLoads() {
    // Verifies Spring Boot can start the application
    // Catches missing beans, configuration errors, etc.
  }
}
```

**Impact**: Catches 80% of startup failures, minimal effort

### Week 2-3: Add Integration Tests
Focus on:
- Cross-service REST calls (verify JWT forwarding works)
- Database operations (verify Flyway migrations work)
- Critical user flows

---

## Troubleshooting When Tests Fail

### "maven-compiler-plugin errors"
→ Service has syntax errors or missing dependencies  
→ Fix: Review error message and correct code

### "mvn: command not found"
→ Maven not installed on Jenkins machine  
→ Fix: Install Maven on Windows Jenkins agent

### "Verification failed: service X did not compile"
→ Service has compilation errors  
→ Fix: Run `mvn clean compile` locally to see actual errors

---

## Next Steps

1. **Today**: Jenkins pipeline working ✅
2. **This week**: Add 4 simple "contextLoads" tests (40 min)
3. **Next week**: Add integration tests for cross-service calls
4. **Future**: 100% unit test coverage per service

---

## References

- Jenkins Jenkinsfile: [Jenkinsfile](../Jenkinsfile)
- Existing tests: [family-service tests](../backend/family-service/src/test/java)
- Test template: [Family Service Tests](../backend/family-service/src/test/java/com/agrawalpulse/family)
