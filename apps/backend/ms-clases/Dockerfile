# Dockerfile para ms-clases
# ETAPA 1: Compilacion con Maven desde el contexto apps/backend
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Se copia todo el backend porque el pom del modulo depende del padre y de otros modulos.
COPY . .

RUN mvn clean package -pl ms-clases -am -DskipTests

# ETAPA 2: Ejecucion ligera con Java 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S siga && adduser -S siga -G siga
USER siga

COPY --from=builder /app/ms-clases/target/ms-clases-*.jar app.jar

EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
