# Auditoria del backend

Revision exhaustiva de los 10 modulos del backend (base `dev`, commit `6204d54`).
Objetivo: dejar por escrito los hallazgos, su impacto y las decisiones pendientes
para que el equipo priorice. Los PRs #49 a #53 ya corrigieron la compilacion del
BFF, el contrato de notas/evaluaciones, las migraciones, Docker/CI y el resolver
de nginx; esos puntos **no** se repiten aqui.

## 1. Resumen ejecutivo

| Prioridad | Tema | Hallazgos |
| --- | --- | --- |
| **P0** | Seguridad y autorizacion | Ownership ausente en 4 flujos, desfase de scopes frontend/backend (**decision pendiente**), `state` editable por APODERADO, fallbacks del BFF inactivos |
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

### 2.2 Decision pendiente: autorizacion (roles vs scopes)

**Como funciona hoy:**

1. El SPA pide token con los scopes de `apps/frontend/public/config.json`
   (solo `api://<client-id>/Acceso.Base`).
2. Entra ID devuelve `scp` (delegado, por aplicacion) y `roles` (app roles, por usuario).
3. `common-properties.yaml` mapea `scp -> SCOPE_` y `roles -> ROLE_`.
4. Los controllers exigen `hasRole(...) and hasAuthority('SCOPE_x:y')`.

**Problema:** un scope es una capacidad de la **aplicacion** (igual para todos los
usuarios del SPA); el unico claim que varia por usuario es el rol. Ademas, los 38
scopes granulares **no estan expuestos ni solicitados**, por lo que hoy todo
endpoint con `hasAuthority('SCOPE_...')` responde **403**, incluido ADMIN.
Referencia: `docs/testing-login.md:33`.

**Opcion A - No eliminarlos (configurarlos):** exponer los 38 scopes en Azure,
agregarlos a `config.json` y dar consentimiento de admin. Funciona, pero todos los
usuarios del SPA reciben los mismos scopes (la autorizacion real sigue siendo por
rol), el token crece y cada endpoint nuevo exige tocar Azure + frontend + consentimiento.

**Opcion B - Eliminarlos (recomendada):** dejar `Acceso.Base` como unico scope
(frontera "la app puede llamar a la API", idealmente exigido en
`SharedSecurityConfig`) y autorizar con `hasRole(...)` + ownership por `oid`.
Cero cambios en Azure, `config.json` ni consentimientos.

**Inventario de los 38 scopes a eliminar:**

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

Impacto en codigo: 68 expresiones `@PreAuthorize` a simplificar. **No se toca
nada hasta que el equipo decida.**

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
8. **Puertos con saltos** (8081, 8082, 8086, 8087, 8088-8091).

## 5. P3 - Calidad

1. **Tests.** Solo `UsuarioServiceTest`; el resto son `contextLoads` excluidos en
   CI, por lo que servicios/controllers no tienen cobertura real.
2. **Encoding inconsistente.** Varios archivos tienen mojibake (`Configuraci��n`,
   `encontr��`, `vǭlido`), lo que puede corromper texto al editar.
3. **Documentacion.** `docs/backend.md:158` dice que `CommonDateFormatConfig` usa
   `dd/MM/yyyy` para JSON y parametros; el API usa ISO `yyyy-MM-dd`
   (`CommonDateFormatConfig.java:20`) y `dd/MM/yyyy` es solo presentacion del frontend
   (correcto en `docs/backend.md:174` y `docs/testing-login.md:198`).
4. **Graph dentro de la transaccion** y `getTokenSync` bloqueante
   (`GraphUserDirectory`).
5. **Sin `@Version`** (optimistic locking) en entidades con contadores.
6. **Dockerfiles** con `COPY . .` sin cache de dependencias y sin healthcheck de app.
7. **Secreto local.** `.env` contiene `AZURE_CLIENT_SECRET` real (rotacion pendiente).

## 6. Plan de correccion propuesto

| PR | Contenido | Depende de |
| --- | --- | --- |
| B | Bugs P1 (docente jefe, soft delete+unique, cupos, carreras, busqueda `state`, deletes) | - |
| A1 | Ownership por `oid`, `idAlumno` autenticado, quitar `state` del update, circuit breaker BFF (**sin tocar `@PreAuthorize`**) | - |
| C1 | `Pageable`/`Page<T>` en los 9 search (size 20, max 100, sort estable) | - |
| E | Frontend `Page<T>` + `siga-paginador` | C1 |
| C2 | Logging/handlers 400/405, naming, indices, seeds, `FeignAuthConfig` | B, A1, C1 |
| D | Tests, encoding, docs, healthchecks | - |
| A2 | Autorizacion (roles + `Acceso.Base` vs 38 scopes) | **decision del equipo** |

## 7. Anexo - verificacion

- Build: `mvn -B -ntp package -DskipTests -Dtypescript-generator.skip=true` en `apps/backend`.
- Stack: `docker compose config`, `docker compose up -d`, health en 8080-8091.
- Migraciones: revisar `docker compose logs ms-*` (`Successfully applied`).
