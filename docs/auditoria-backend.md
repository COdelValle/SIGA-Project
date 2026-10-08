# Auditoria del backend

Revision exhaustiva de los 10 modulos del backend (base `dev`, commit `6204d54`).
Objetivo: dejar por escrito los hallazgos, su impacto y las decisiones pendientes
para que el equipo priorice. Los PRs #49 a #53 ya corrigieron la compilacion del
BFF, el contrato de notas/evaluaciones, las migraciones, Docker/CI y el resolver
de nginx; esos puntos **no** se repiten aqui. La seccion 8 resume el estado de
remediacion (PRs #55 a #68) y lo que queda pendiente.

## 1. Resumen ejecutivo

| Prioridad | Tema | Hallazgos |
| --- | --- | --- |
| **P0** | Seguridad y autorizacion | Ownership ausente en 4 flujos, scopes granulares mantenidos (config pendiente en TI), `state` editable por APODERADO, fallbacks del BFF inactivos |
| **P1** | Bugs funcionales | Docente jefe, soft delete + UNIQUE, criterio de cupos, condiciones de carrera, busqueda por `state`, deletes inconsistentes, duplicados en update |
| **P2** | Consistencia y observabilidad | 500 en errores que deberian ser 400/405, sin paginacion, naming/enums/soft-delete dispares, sin indices, seeds con IDs fijos, codigo duplicado |
| **P3** | Calidad | Cobertura de tests casi nula, encoding inconsistente, documentacion desactualizada, healthchecks ausentes |

## 2. P0 - Seguridad y autorizacion

### 2.1 Ownership ausente (autoservicio por rol)

Los endpoints que permiten rol APODERADO/DOCENTE/ESTUDIANTE no validan que el
recurso pertenezca al usuario autenticado (no se usa `SecurityUtils` fuera de
usuarios/me/Feign):

| Flujo | Riesgo | Evidencia |
| --- | --- | --- |
| Actualizar estudiante | Un APODERADO puede editar **cualquier** estudiante | `apps/backend/ms-estudiantes/.../service/EstudianteService.java:76-87` |
| Actualizar apoderado | Un APODERADO puede editar **cualquier** apoderado | `apps/backend/ms-apoderados/.../service/ApoderadoService.java:79-86` |
| Certificados | Un DOCENTE puede agregar/borrar certificados de **cualquier** docente | `apps/backend/ms-docentes/.../service/CertificadoService.java:36-45,47-62` |
| Inscripcion | Un ESTUDIANTE puede inscribir a **cualquier** `idAlumno` | `apps/backend/ms-asignaturas/.../service/InscripcionService.java:53-83` y `RegistrarInscripcionRequestDTO` |

**Recomendacion:** validar contra el claim `oid` del JWT (`SecurityUtils.getCurrentUserOid()`)
comparando con `idUsuario` de la entidad (apoderado/docente/estudiante). Para la
inscripcion, resolver el estudiante interno por `oid` (Feign a `ms-estudiantes`)
y rechazar si el body trae otro `idAlumno`. La actualizacion de estudiante por
APODERADO requiere el vinculo apoderado-estudiante (vive en `ms-apoderados`);
hacerlo desde `ms-estudiantes` crearia un ciclo de dependencias, por lo que se
propone restringir a ADMIN o al propio estudiante y definir el flujo de apoderado
por separado.

### 2.2 Decision tomada: se mantienen los scopes granulares

**Como funciona:**

1. El SPA pide token con los scopes de `apps/frontend/public/config.json`
   (`Acceso.Base` + los 42 granulares).
2. Entra ID devuelve `scp` (delegado, por aplicacion) y `roles` (app roles, por usuario).
3. `common-properties.yaml` mapea `scp -> SCOPE_` y `roles -> ROLE_`.
4. Los controllers exigen `hasRole(...) and hasAuthority('SCOPE_x:y')`.

**Decision del equipo:** los scopes granulares se **mantienen** (son un requisito del
proyecto, aunque hoy sean codigo muerto si el tenant no los expone). Politica
unificada:

- **Lecturas:** `hasAuthority('SCOPE_x:read')` (usuario autenticado).
- **Escrituras:** `hasRole(...) and hasAuthority('SCOPE_x:write|update|delete')`,
  **incluido ADMIN** (ya no hay atajos `or` que dejen a ADMIN sin scope).
- **Ownership por `oid`** para autoservicio (APODERADO/DOCENTE/ESTUDIANTE).

**Pendiente en TI (Azure):** exponer los 42 scopes en la app de API, agregarlos al
consentimiento de admin y mantenerlos en `config.json`. Hasta entonces, los
endpoints con scope responden **403** con los tokens actuales. Checklist completo
en `docs/testing-login.md`.

**Inventario de los 42 scopes (se mantienen):**

| Servicio | Scopes |
| --- | --- |
| usuarios | read, write, update, delete |
| estudiantes | read, write, update, delete |
| asignaturas | read, write, update, delete |
| notas | read, write, update, delete |
| docentes | read, write, update, delete |
| apoderados | read, write, update, delete |
| clases | read, write, update, delete |
| evaluaciones | read, write, update, delete |
| horarios | write, update, delete |
| inscripciones | read, write, update |
| asistencias | read, write, update, delete |

Impacto en codigo: 68 expresiones `@PreAuthorize` unificadas (9 cambiadas para que
ADMIN tambien requiera scope) + ownership en `updateDocente`.

### 2.3 Otros hallazgos P0

- **`state` en `ActualizarEstudianteRequestDTO`:** un APODERADO puede dar de baja
  o reactivar un estudiante via PUT (duplica el DELETE). `apps/backend/libs/core-share/.../dto/estudiante/ActualizarEstudianteRequestDTO.java:23-24`.
- **Fallbacks del BFF inactivos:** falta `spring.cloud.openfeign.circuitbreaker.enabled: true`
  en `apps/backend/bff-web/src/main/resources/application.yaml`; un fallo de un MS
  en el perfil devuelve 500 (FeignException) en vez de 503.
- **OData en Graph:** el email se concatena en `$filter`
  (`GraphUserDirectory.findUserByEmail`), mitigado por `@Email` pero evitable con
  parametros escapados.
- **`FeignClientConfig` del BFF** es `@Configuration` escaneada globalmente
  (aunque se pase por `configuration=`); conviene moverla a un paquete no escaneado.

## 3. P1 - Bugs funcionales

1. **Clase: mismo docente jefe falla.** `existsByIdDocenteJefeAndActiveTrue`
   incluye la propia clase: reasignar el docente actual lanza "ya es jefe de otra
   clase activa". `ClaseService.java:81-84`. Ademas
   `ActualizarDocenteJefeRequestDTO` es `@NotNull`, asi que no se puede limpiar
   el jefe aunque la entidad lo permite.
2. **Soft delete + UNIQUE.** `uk_nivel_letra_anio` (clases) y `uk_asignatura_name`
   (asignaturas) bloquean recrear una fila "eliminada": el chequeo del servicio
   filtra activos pero el indice no. `ClaseService.java:51`,
   `AsignaturaBasicaService.java:40`, migraciones `V1`.
3. **Criterios de cupo distintos.** Cupos disponibles cuenta `ACTIVO/PRE_INSCRITO`;
   la reduccion de cupo cuenta **todas** las inscripciones (incluye `CANCELADO`).
   `InscripcionService.java:101-108` vs `AsignaturaElectivaService.java:63-69`.
4. **Condiciones de carrera.** `count -> insert` de cupos y `existsBy -> save`
   (RUT/email/`idUsuario`, sin unique en `id_usuario`) y suma de ponderaciones.
   `EvaluacionService.java:94-108`.
5. **Busqueda de estudiantes por `state`.** `isActive()` (state != INACTIVO) se
   combina con `hasState(state)`: pedir `state=INACTIVO` siempre da vacio.
   `EstudianteService.java:45-55`, `EstudianteSpecifications.java:11-13,70-78`.
6. **Horarios.** Sin validacion de solapamiento; `eliminarHorario` es hard delete
   (inconsistente con el soft delete del resto) y no protege el minimo de uno.
   `HorarioService.java:54-66`.
7. **Deletes inconsistentes.** `NotaService.deleteNota` usa `findById` (repetir
   devuelve 204) mientras get/update usan `findByIdAndActiveTrue`;
   `NotaService.java:65-81`.
8. **Usuarios.** `updateUsuario` permite `state=INACTIVO` por PUT y que un ADMIN
   se desactive a si mismo. `UsuarioService.java:111-143`.
9. **Evaluaciones.** `updateEvaluacion` no valida nombre duplicado (save si);
   las electivas no validan duplicado y `updateBasica` no revalida.
   `EvaluacionService.java:66-75`, `AsignaturaElectivaService.java:39-48`.
10. **Certificados.** El orden de validaciones puede responder 400 en vez de 404
    si el certificado pertenece a otro docente. `CertificadoService.java:47-62`.
11. **Validacion de negocio en `@PrePersist`** lanzando `IllegalStateException`
    (`AsignaturaBasica.idClase`, `AsignaturaElectiva.cupoMaximo`) -> 500 en vez de 400.
12. **Inscripcion no valida existencia del alumno** (no hay Feign a `ms-estudiantes`),
    a diferencia de apoderados/notas.

## 4. P2 - Consistencia y observabilidad

1. **Manejo de errores.** `GlobalExceptionHandler.handleGeneric` no loggea la
   excepcion; el catch-all `Exception` convierte en 500 errores que deberian ser
   400/405 (`MethodArgumentTypeMismatch`, `MissingServletRequestParameter`,
   `HttpRequestMethodNotSupported`). `GlobalExceptionHandler.java:105-111`.
2. **Sin paginacion.** Los 9 `GET /search` devuelven `List` sin limite ni orden
   estable: usuarios, estudiantes, asignaturas, inscripciones, notas, docentes,
   apoderados, clases, evaluaciones.
3. **Naming/config dispar.** El BFF usa `microservices.*`; los MS usan `services.*`.
   Paquete `integration.Notas` en mayuscula. Enums dispersos (`coreshare.enums`,
   `dto.estudiante.enums`, `dto.usuario.enums`). Soft delete mezcla `active/activo`
   con `State`.
4. **DTOs.** Typo `RegistrarAdoderadoRequestDTO`; `List<ParentescoEstudianteDTO>`
   sin `@Valid` (docentes si lo tiene); `@Size(min = 0)` redundantes.
5. **Seeds con IDs fijos** en docentes/apoderados/clases/evaluaciones + `INSERT IGNORE`
   (saltos silenciosos ante colisiones).
6. **Sin indices** en columnas de busqueda/FK logicas de los MS nuevos:
   `docentes.id_usuario`, `apoderados.id_usuario`, `notas.id_estudiante`,
   `notas.id_evaluacion`, `evaluaciones.id_asignatura`, `asignaturas.id_docente`,
   y sin unique en `apoderado_estudiantes(apoderado_id, estudiante_id)`.
7. **Codigo duplicado.** `FeignAuthConfig` identico en 4 MS; `Specifications.isActive()`
   repetido en 6 MS.
8. **Puertos**: renumerados a `8081/8083-8089`; `8082` y `8090` quedan reservados para `ms-auditoria`/`ms-asistencias`.

## 5. P3 - Calidad

1. **Tests.** 25 unitarios + 8 de contrato de errores + 2 de integracion con
   Testcontainers; faltan pruebas de contrato end-to-end y de controllers.
2. **Encoding**: **falso positivo** verificado: el codigo esta en UTF-8 valido
   (0 caracteres U+FFFD); el "mojibake" era de la consola de PowerShell.
3. **Documentacion.** Corregido: `docs/backend.md` indica ISO `yyyy-MM-dd` y
   `dd/MM/yyyy` como presentacion del frontend.
4. **Graph dentro de la transaccion.** Corregido: la sincronizacion/revocacion de
   roles se ejecuta en `afterCommit`; `getTokenSync` se mantiene (el SDK cachea el token).
5. **Sin `@Version`**: se mantiene; las carreras se cubren con **locks pesimistas**
   (cupos y ponderacion).
6. **Dockerfiles.** Corregido: cache de Maven (`--mount=type=cache`) + healthchecks
   en compose.
7. **Secreto local.** Pendiente manual: rotar `AZURE_CLIENT_SECRET` del `.env`.

## 6. Plan de correccion propuesto

| PR | Contenido | Depende de |
| --- | --- | --- |
| B | Bugs P1 (docente jefe, soft delete+unique, cupos, carreras, busqueda `state`, deletes) | - |
| A1 | Ownership por `oid`, `idAlumno` autenticado, quitar `state` del update, circuit breaker BFF (**sin tocar `@PreAuthorize`**) | - |
| C1 | `Pageable`/`Page<T>` en los 9 search (size 20, max 100, sort estable) | - |
| E | Frontend `Page<T>` + `siga-paginador` | C1 |
| C2 | Logging/handlers 400/405, naming, indices, seeds, `FeignAuthConfig` | B, A1, C1 |
| D | Tests, encoding, docs, healthchecks | - |
| A2 | Autorizacion (se mantienen los 42 scopes; politica unificada) | **Resuelto en codigo; config Azure pendiente en TI** |

## 7. Anexo - verificacion

- Build: `mvn -B -ntp package -DskipTests -Dtypescript.generator.skip=true` en `apps/backend`.
- Stack: `docker compose config`, `docker compose up -d`, health en 8080-8088.
- Migraciones: revisar `docker compose logs ms-*` (`Successfully applied`).

## 8. Estado de remediacion (PRs abiertos)

| Hallazgo | Estado | PR |
| --- | --- | --- |
| P1 - bugs funcionales (docente jefe, soft delete+unique, cupos, carreras, busqueda `state`, horarios, certificados) | Corregido | #55 |
| P0 - ownership por `oid`, `idAlumno` autenticado, `state` fuera del update, FeignAuth en `ms-asignaturas`, circuit breaker del BFF | Corregido (sin tocar `@PreAuthorize`) | #56 |
| P2 - paginacion completa (`Page<T>`, size 20/max 100) | Corregido | #57 (backend) y #58 (frontend) |
| P2 - manejo de errores 400/405 con logging, `FeignAuthConfig` compartido, naming BFF, indices | Corregido | #59 |
| P3 - tests unitarios (25), healthchecks, DTO de apoderado, docs de fechas | Corregido | #60 |
| P0 - autorizacion: se mantienen los 42 scopes granulares y se unifica la politica (ADMIN tambien requiere scope) | Resuelto en codigo; **exponer/consentir scopes en Azure (TI)** | #62 |
| P2 - puertos con saltos | Renumerados a `8081/8083-8089`; reservados `8082`/`8090` | #63 |
| P0/P2/P3 - OData, BFF sin config escaneada, unique `id_usuario`, `PropertyReferenceException` 400, `conCupoDisponible`, DTOs, correlation ID, `ErrorResponseDTO` con `path`/`correlationId`, Graph `afterCommit` | Corregido | #64 |
| P1 - horarios con hard delete | Soft delete real (minimo 1 activo) | #65 |
| P0 - flujo apoderado -> pupilo | Endpoint BFF con validacion de vinculo | #66 |
| P3 - tests de integracion/contrato | Handler de errores + Testcontainers (migraciones/unique/soft delete) | #67 |
| P3 - Dockerfiles sin cache | Cache de Maven en los 9 Dockerfiles | #68 |

Nota: mientras TI no exponga y consienta los scopes, los endpoints con
`hasAuthority('SCOPE_...')` siguen respondiendo 403 con los tokens actuales.
