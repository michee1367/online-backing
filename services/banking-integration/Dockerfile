# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY shared/ shared/
COPY services/banking-integration/ services/banking-integration/
RUN apk add --no-cache maven && \
    mvn package -pl services/banking-integration -am -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S villagesat && adduser -S villagesat -G villagesat
WORKDIR /app
COPY --from=builder /app/services/banking-integration/target/*.jar app.jar

USER villagesat
EXPOSE 8093

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget -qO- http://localhost:8093/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", \
  "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
