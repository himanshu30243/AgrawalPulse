# Multi-stage build. Builds from the backend/ reactor root so the Maven build can resolve the
# common module as a reactor dependency, then run only analytics-service's tests/package.
#
# Build context must be backend/, e.g.:
#   docker build -f analytics-service/Dockerfile -t agrawalpulse/analytics-service .
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY family-service/pom.xml family-service/pom.xml
COPY membership-service/pom.xml membership-service/pom.xml
COPY matrimony-service/pom.xml matrimony-service/pom.xml
COPY event-service/pom.xml event-service/pom.xml
COPY analytics-service/pom.xml analytics-service/pom.xml
COPY mvnw .
COPY .mvn .mvn

COPY common/src common/src
COPY analytics-service/src analytics-service/src

RUN ./mvnw -q -pl analytics-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
COPY --from=build /workspace/analytics-service/target/analytics-service.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
