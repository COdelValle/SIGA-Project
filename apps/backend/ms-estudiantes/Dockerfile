# Dockerfile de ejemplo para el backend
# ETAPA 1: Compilación con Maven desde el contexto apps/backend
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copiar el POM padre y las fuentes del módulo específico
COPY pom.xml .
COPY ms-estudiantes/pom.xml ms-estudiantes/
COPY ms-estudiantes/src ms-estudiantes/src

# Compilar el módulo e incluir dependencias del padre sin ejecutar tests
RUN mvn clean package -pl ms-estudiantes -am -DskipTests

# ETAPA 2: Ejecución ligera con Java 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S siga && adduser -S siga -G siga
USER siga

COPY --from=builder /app/ms-estudiantes/target/ms-estudiantes-*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]