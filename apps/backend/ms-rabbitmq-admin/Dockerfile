# syntax=docker/dockerfile:1
# Dockerfile para ms-rabbitmq-admin
# ETAPA 1: Compilacion con Maven desde el contexto apps/backend
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Se copia todo el backend porque el pom del modulo depende del padre y de otros modulos.
COPY . .

RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl ms-rabbitmq-admin -am -DskipTests

# ETAPA 2: Ejecucion ligera con Java 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S siga && adduser -S siga -G siga
USER siga

COPY --from=builder /app/ms-rabbitmq-admin/target/ms-rabbitmq-admin-*.jar app.jar

EXPOSE 8092
ENTRYPOINT ["java", "-jar", "app.jar"]
