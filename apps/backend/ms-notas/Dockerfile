# syntax=docker/dockerfile:1
# Dockerfile para ms-notas
# ETAPA 1: Compilación con Maven desde el contexto apps/backend
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Se copia todo el backend porque el pom del módulo depende del padre y de otros módulos.
COPY . .

RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl ms-notas -am -DskipTests

# ETAPA 2: Ejecución ligera con Java 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S siga && adduser -S siga -G siga
USER siga

COPY --from=builder /app/ms-notas/target/ms-notas-*.jar app.jar

EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
