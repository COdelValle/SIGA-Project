FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY . .

RUN mvn clean package -pl ms-kafka-admin -am -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S siga && adduser -S siga -G siga

USER siga

COPY --from=builder /app/ms-kafka-admin/target/ms-kafka-admin-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8095

ENTRYPOINT ["java","-jar","app.jar"]
