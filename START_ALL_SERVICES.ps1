# AgrawalPulse Microservices - Startup Script
# Starts all 11 services in the correct order
# Requirements: Maven installed, PostgreSQL running

Write-Host "========================================" -ForegroundColor Green
Write-Host "AgrawalPulse Microservices Startup" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# Check prerequisites
Write-Host "[1/3] Checking Prerequisites..." -ForegroundColor Yellow

# Check Maven
try {
    $mvnVersion = & mvn --version 2>&1 | Select-Object -First 1
    Write-Host "✓ Maven found: $mvnVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Maven not found. Please install Maven." -ForegroundColor Red
    exit 1
}

# Check PostgreSQL
Write-Host "✓ Please ensure PostgreSQL is running on localhost:5432" -ForegroundColor Green
Write-Host ""

# Navigate to backend directory
cd "e:\Himanshu\Workspace\AgrawalPulse\backend"

# Build all modules
Write-Host "[2/3] Building all modules..." -ForegroundColor Yellow
& mvn clean install -DskipTests=true -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Build failed" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Build successful" -ForegroundColor Green
Write-Host ""

# Start services
Write-Host "[3/3] Starting Services..." -ForegroundColor Yellow
Write-Host ""
Write-Host "IMPORTANT:" -ForegroundColor Cyan
Write-Host "  Jenkins is running on port 8080" -ForegroundColor Cyan
Write-Host "  API Gateway will run on port 8090 (changed from 8080)" -ForegroundColor Cyan
Write-Host ""
Write-Host "Start these in separate terminals in this order:" -ForegroundColor Cyan
Write-Host ""

$services = @(
    @{Name="EUREKA SERVER"; Port="8761"; Command="mvn -pl eureka-server spring-boot:run"},
    @{Name="CONFIG SERVER"; Port="8888"; Command="mvn -pl config-server spring-boot:run"},
    @{Name="API GATEWAY"; Port="8090"; Command="mvn -pl api-gateway spring-boot:run -Dspring-boot.run.profiles=local"},
    @{Name="USER SERVICE"; Port="8081"; Command="mvn -pl user-service spring-boot:run -Dspring-boot.run.profiles=local"},
    @{Name="FAMILY SERVICE"; Port="8082"; Command="mvn -pl family-service spring-boot:run -Dspring-boot.run.profiles=local"},
    @{Name="MEMBERSHIP SERVICE"; Port="8083"; Command="mvn -pl membership-service spring-boot:run -Dspring-boot.run.profiles=local"},
    @{Name="MATRIMONY SERVICE"; Port="8084"; Command="mvn -pl matrimony-service spring-boot:run -Dspring-boot.run.profiles=local"},
    @{Name="EVENT SERVICE"; Port="8085"; Command="mvn -pl event-service spring-boot:run -Dspring-boot.run.profiles=local"},
    @{Name="ANALYTICS SERVICE"; Port="8086"; Command="mvn -pl analytics-service spring-boot:run -Dspring-boot.run.profiles=local"}
)

$counter = 1
foreach ($service in $services) {
    Write-Host "Terminal $counter - $($service.Name) (port $($service.Port)):" -ForegroundColor Cyan
    Write-Host "  cd e:\Himanshu\Workspace\AgrawalPulse\backend" -ForegroundColor White
    Write-Host "  $($service.Command)" -ForegroundColor White
    Write-Host ""
    $counter++
}

Write-Host "========================================" -ForegroundColor Green
Write-Host "Startup URLs" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Eureka Dashboard:        http://localhost:8761" -ForegroundColor Cyan
Write-Host "Config Server:           http://localhost:8888" -ForegroundColor Cyan
Write-Host "API Gateway:             http://localhost:8090" -ForegroundColor Cyan
Write-Host ""
Write-Host "Health Checks:" -ForegroundColor Cyan
Write-Host "  Gateway:     curl http://localhost:8090/health" -ForegroundColor White
Write-Host "  User Service: curl http://localhost:8081/actuator/health" -ForegroundColor White
Write-Host "  Family Service: curl http://localhost:8082/actuator/health" -ForegroundColor White
Write-Host ""
Write-Host "Test Request:" -ForegroundColor Cyan
Write-Host "  curl -X GET http://localhost:8090/api/v1/users \\" -ForegroundColor White
Write-Host "    -H \"Authorization: Bearer YOUR_JWT_TOKEN\"" -ForegroundColor White
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Build complete! Ready to start services." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Press any key to continue..." -ForegroundColor Yellow
Read-Host
