FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY shared/ shared/
COPY services/user-service/ services/user-service/
RUN apk add --no-cache maven && mvn package -pl services/user-service -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S villagesat && adduser -S villagesat -G villagesat
WORKDIR /app
COPY --from=builder /app/services/user-service/target/*.jar app.jar
USER villagesat
EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
