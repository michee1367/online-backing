# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY shared/ shared/
COPY services/auth-service/ services/auth-service/
RUN apk add --no-cache maven && \
    mvn package -pl services/auth-service -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S villagesat && adduser -S villagesat -G villagesat
WORKDIR /app
COPY --from=builder /app/services/auth-service/target/*.jar app.jar
USER villagesat
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
