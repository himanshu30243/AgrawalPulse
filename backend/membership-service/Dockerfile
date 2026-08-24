FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY membership-service/pom.xml membership-service/pom.xml
RUN mvn -q -pl membership-service -am -DskipTests dependency:go-offline

COPY common/src common/src
COPY membership-service/src membership-service/src
RUN mvn -q -pl membership-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /workspace/membership-service/target/membership-service-*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
