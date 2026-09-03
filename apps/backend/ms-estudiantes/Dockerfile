# Dockerfile de ejemplo para el backend
# ETAPA 1: Compilación con Maven desde el contexto apps/backend
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Se necesita copiar todo el backend en cada microservicio, ya que el pom.xml del microservicio depende de los módulos padre y otros módulos, aunque no se compilen en este microservicio. Esto es necesario para que Maven pueda resolver las dependencias correctamente.
COPY . .

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