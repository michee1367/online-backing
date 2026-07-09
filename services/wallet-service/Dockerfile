# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY shared/ shared/
COPY services/wallet-service/ services/wallet-service/
RUN apk add --no-cache maven && \
    mvn package -pl services/wallet-service -am -DskipTests -B

# Runtime stage — minimal attack surface
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S villagesat && adduser -S villagesat -G villagesat
WORKDIR /app
COPY --from=builder /app/services/wallet-service/target/*.jar app.jar

USER villagesat
EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget -qO- http://localhost:8083/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", \
  "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
