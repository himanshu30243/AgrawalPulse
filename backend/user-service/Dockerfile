# Multi-stage build. Build context MUST be backend/ (the reactor root), so the Maven
# wrapper can resolve every module declared in backend/pom.xml - not just this one -
# even though only user-service is actually packaged. e.g.:
#   docker build -f user-service/Dockerfile -t agrawalpulse/user-service .
#
# User identity, auth, roles.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Whole reactor first (poms + wrapper), so `docker build` fails fast and clearly if a
# module is missing here, instead of failing deep inside a Maven reactor scan.
COPY . .

RUN ./mvnw -q -pl user-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
COPY --from=build /workspace/user-service/target/user-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
