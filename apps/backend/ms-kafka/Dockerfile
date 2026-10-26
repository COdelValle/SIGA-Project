FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY . .

RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl ms-kafka -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S siga && adduser -S siga -G siga
USER siga

COPY --from=builder /app/ms-kafka/target/ms-kafka-*.jar app.jar

EXPOSE 8095
ENTRYPOINT ["java", "-jar", "app.jar"]
