# SIGA-Project

## Resumen ejecutivo

SIGA-Project es un proyecto de gestión académica en construcción. La intención del sistema es administrar estudiantes, docentes, usuarios, asignaturas y notas dentro de una arquitectura modular y escalable.

A partir de la estructura actual del repositorio, el proyecto ya cuenta con una base técnica organizada, pero aún no está en una etapa funcional completa. Hay un frontend Angular, varios microservicios Spring Boot y una infraestructura base en Docker/Terraform, pero la implementación real del dominio académico todavía está pendiente.

## Objetivo del proyecto

Construir un sistema integral para la gestión académica institucional, con separación por módulos y capacidad de crecer en microservicios. El enfoque busca:

- gestionar usuarios y autenticación
- administrar estudiantes
- gestionar asignaturas y docentes
- registrar notas
- centralizar la capa de frontend mediante un BFF o gateway
- preparar la base para despliegue y operación real

## Estado actual del repositorio

El repositorio ya está estructurado como proyecto de software con varias piezas clave preparadas:

- Frontend Angular en `apps/frontend`
- Backend multi-módulo Maven en `apps/backend`
- Microservicios con Spring Boot
- Biblioteca compartida `java-common-dto`
- Configuración de seguridad
- Documentación API con Swagger y Scalar
- Docker Compose base
- Terraform inicial con archivo `main.tf` sin contenido real todavía

Sin embargo, el proyecto todavía se encuentra en una etapa inicial de scaffolding y arquitectura, no en una versión funcional terminada.

## Estructura del proyecto

```text
SIGA-Project/
├── apps/
│   ├── backend/
│   │   ├── pom.xml
│   │   ├── bff-web/
│   │   ├── ms-usuarios-auth/
│   │   ├── ms-estudiantes/
│   │   ├── ms-asignaturas/
│   │   ├── ms-notas/
│   │   └── libs/
│   │       └── java-common-dto/
│   └── frontend/
│       ├── package.json
│       ├── angular.json
│       ├── README.md
│       └── src/
├── docker-compose.yml
├── terraform/
│   └── main.tf
├── README.md
└── .gitignore
```

## Frontend

### Ubicación
`apps/frontend`

### Tecnologías
- Angular 22
- TypeScript
- RxJS
- Angular Router

### Estado
El proyecto frontend se generó con Angular CLI y contiene la estructura base correcta, pero todavía aparace como una aplicación tipo plantilla por defecto. El contenido de `app.html` y `app.ts` no refleja aún la lógica funcional del sistema académico.

### Observación importante
El frontend tiene la base necesaria para comenzar a desarrollar pantallas, pero aún no se ve la implementación real de módulos, servicios ni flujo de negocio.

## Backend

### Ubicación
`apps/backend`

### Tecnología principal
- Java 21
- Spring Boot 3.5
- Maven
- Spring Security
- Spring Cloud
- Feign
- JPA / Hibernate

### Módulos detectados
- `bff-web`
- `ms-usuarios-auth`
- `ms-estudiantes`
- `ms-asignaturas`
- `ms-notas`
- `libs/java-common-dto`

### POM principal
El `pom.xml` del backend define un proyecto padre con:

- Java 21
- Spring Boot 3.5
- módulos organizados
- gestión centralizada de dependencias
- soporte para Lombok, MapStruct y OpenAPI
- dependencia de `java-common-dto`

Esto indica una buena base para crecer desde una arquitectura modular.

## Microservicios actuales

### 1) `ms-usuarios-auth`
Responsabilidad esperada:
- autenticación
- usuarios
- roles
- integración con Azure AD / JWT

Se observa configuración de seguridad y documentación Swagger/Scalar. El servicio ya está preparado para trabajar con JWT, pero aún no hay evidencia de lógica de negocio real.

### 2) `ms-estudiantes`
Responsabilidad esperada:
- CRUD de estudiantes
- perfiles
- alergias
- matrícula
- datos académicos del estudiante

Ya hay la estructura base del microservicio y la configuración de seguridad.

### 3) `ms-asignaturas`
Responsabilidad esperada:
- gestión de asignaturas
- relación con docentes
- curso y programas

La configuración incluye datasource, JWT OAuth2, Feign y Swagger.

### 4) `ms-notas`
Responsabilidad esperada:
- registro y consulta de notas
- relación con estudiantes y asignaturas

Este microservicio ya incorpora configuración útil para:
- datasource MariaDB
- OAuth2 JWT
- Feign
- integración con otros servicios

### 5) `bff-web`
Responsabilidad esperada:
- orquestación de llamadas a otros servicios
- capa de frontend / gateway
- centralización de acceso para UI

Se configura con integración a varios microservicios y seguridad JWT.

## Biblioteca compartida

### `libs/java-common-dto`
Esta librería existe como capa de modelos compartidos. Actualmente solo se observa un DTO base:

- `EstudianteDTO`

Esto es una buena señal de que el proyecto va encaminado a centralizar contratos de intercambio entre servicios. Sin embargo, aún falta consolidar más DTOs y un conjunto más amplio de modelos.

## Seguridad y documentación

Se observa una configuración consistente para seguridad y documentación:

- `springdoc`
- Swagger UI
- Scalar UI
- rutas públicas de documentación
- autenticación con OAuth2 JWT para backend
- acceso restringido al resto de endpoints

Ejemplo de rutas documentadas:
- `/v3/api-docs`
- `/docs/swagger`
- `/docs/scalar`

Esto indica que el equipo ya está pensando en buena operatividad y documentación técnica desde el inicio.

## Contenedores y despliegue

### Docker Compose
Archivo: `docker-compose.yml`

Contiene una primera base con:
- red Docker
- volumen para MariaDB
- un servicio de ejemplo para `ms-estudiantes`

Todavía no está completo como orquestación del sistema real, pero es una base útil para continuar.

### Terraform
Archivo: `terraform/main.tf`

Actualmente no hay contenido funcional real. Esto significa que la infraestructura como código aún no está implementada.

## Variables de entorno esperadas

La configuración del backend hace referencia a variables como:

- `DB_HOST`
- `DB_USER`
- `DB_PASS`
- `AZURE_TENANT_ID`
- `MS_ESTUDIANTES_URL`
- `MS_ASIGNATURAS_URL`
- `MS_NOTAS_URL`
- `MS_ASISTENCIAS_URL`

Esto evidencia que la intención es usar:
- base de datos MariaDB
- JWT de Azure AD
- llamadas internas entre microservicios

## Lo que sí está bien hecho

Entre los puntos positivos del proyecto actual:

- la estructura está organizada por módulos
- existe separación clara entre frontend y backend
- hay arquitectura de microservicios pensada
- los microservicios tienen base para Spring Boot
- hay configuración de seguridad y documentación
- se prepara la integración con bases de datos y JWT
- la librería compartida ya está en camino

## Lo que falta todavía para que funcione como sistema real

Aún no está completamente implementado lo siguiente:

- entidades de dominio reales
- repositorios JPA
- servicios de negocio
- controladores REST
- DTOs de cada módulo
- validaciones de negocio
- pruebas unitarias y de integración
- endpoints concretos para cada funcionalidad
- integración real entre frontend y backend
- despliegue completo con Docker Compose
- infraestructura Terraform real
- configuración final de datos en entorno

## Diagnóstico práctico

El proyecto está en una fase de base arquitectónica y preparación técnica. Es decir:

- no es un sistema terminado
- sí tiene una estructura bastante buena
- sí tiene una intención clara de arquitectura
- aún falta la parte de negocio y funcionalidad real

En términos de avance, podríamos decir que está “listo para construir sobre él”, pero no “listo para entregar”.

## Recomendación de trabajo

Para avanzar de forma ordenada, el siguiente paso debería ser:

1. definir el dominio académico completo
2. crear entidades por microservicio
3. poner DTOs y contratos de API
4. implementar repositorios y servicios
5. crear controladores REST básicos
6. conectar frontend con los endpoints reales
7. completar Docker Compose
8. preparar Infraestructura Terraform
9. añadir pruebas y validaciones
10. documentar cada módulo y flujo

## Conclusión

El proyecto tiene una base sólida y un diseño orientado a microservicios con una buena intención técnica. La parte más importante ya está posada: estructura, módulos, observabilidad, seguridad, documentación y preparación para integrar servicios.

Lo que todavía hace falta es la construcción funcional del sistema: negocio real, integración entre servicios, frontend conectado y despliegue final. En otras palabras, el proyecto ya tiene “arquitectura”, pero todavía no tiene “funcionalidad completa”.

## Nota para compañeros

Si vamos a compartir esta situación con el equipo, esta es la forma más clara de resumirla:

- la base está bien hecha
- falta enfocar la lógica del negocio
- hay que priorizar endpoints y flujos de usuario
- el siguiente sprint debería centrarse en dominio y vinculación real entre módulos