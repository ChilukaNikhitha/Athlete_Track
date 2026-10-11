FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package
COPY src/main/docker/HealthProbe.java /tmp/HealthProbe.java
RUN javac -d /health-probe /tmp/HealthProbe.java

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/athletetrack-api-1.0.0.jar app.jar
COPY --from=build /health-probe /app/health-probe
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD ["java", "-cp", "/app/health-probe", "HealthProbe", "http://localhost:8080/api/health"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
