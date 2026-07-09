FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY shared/ shared/
COPY services/compliance-service/ services/compliance-service/
RUN apk add --no-cache maven && mvn package -pl services/compliance-service -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S villagesat && adduser -S villagesat -G villagesat
WORKDIR /app
COPY --from=builder /app/services/compliance-service/target/*.jar app.jar
USER villagesat
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
