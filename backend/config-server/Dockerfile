# Multi-stage build. Build context MUST be backend/ (the reactor root), so the Maven
# wrapper can resolve every module declared in backend/pom.xml - not just this one -
# even though only config-server is actually packaged. e.g.:
#   docker build -f config-server/Dockerfile -t agrawalpulse/config-server .
#
# Centralized Spring Cloud Config server.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Whole reactor first (poms + wrapper), so `docker build` fails fast and clearly if a
# module is missing here, instead of failing deep inside a Maven reactor scan.
COPY . .

RUN ./mvnw -q -pl config-server -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
COPY --from=build /workspace/config-server/target/config-server-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
