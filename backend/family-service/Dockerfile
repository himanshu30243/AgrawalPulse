# Multi-stage build: compile the whole reactor (family-service needs the common module built
# and installed first, since it's a regular Maven module dependency, not a fat-jar merge) then
# ship only the runtime layer.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common ./common
COPY family-service ./family-service
RUN mvn -q -pl family-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/family-service/target/family-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
