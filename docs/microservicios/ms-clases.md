# `ms-clases`

**Módulo:** `apps/backend/ms-clases` · **Puerto:** 8087 · **BD:** `siga_clases_db`.

## Responsabilidad y datos

Gestiona clases/cursos por nivel, letra y año académico, estado y docente jefe. `Clase` tiene unicidad funcional nivel+letra+año; el servicio es dueño del esquema y usa JPA/Flyway.

Fuentes principales: `controller/ClaseController.java`, `service/ClaseService.java`, `repository/ClaseRepository.java`, `model/entity/Clase.java` y `client/DocenteClient.java`.

## Dependencias declaradas

[`pom.xml`](../../apps/backend/ms-clases/pom.xml): Web, Validation, Data JPA/MariaDB, Flyway, Security/OAuth2 Resource Server, OpenFeign/Resilience4j, Springdoc, Actuator, MapStruct y `core-share`.

## Acoplamientos y reglas

- `DocenteClient` consulta `ms-docentes` para validar asignación del profesor jefe; es una dependencia síncrona.
- El BFF llama este servicio para listados de clase y para componer el perfil de estudiante.
- CRUD, búsqueda, `exists` y cambio del docente jefe están bajo `/api/v1/clases`. Soft delete conserva histórico; validación evita conflictos con clases activas.
- `ms-asignaturas` requiere clase para crear una dictación; `ms-estudiantes` puede almacenar la referencia lógica de la clase sin FK cross-database.

## Pruebas y operación

Las pruebas del módulo cubren unicidad, asignación y reglas del servicio. Si `ms-docentes` está indisponible, Feign/circuit breaker afecta las operaciones que validan docente; el catálogo/consultas que no necesiten esa llamada no deben confundirse con un fallback de BD. Actuator expone salud.
