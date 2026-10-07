# Registro de pruebas y pasos

Bitácora reutilizable para dejar constancia de las pruebas ejecutadas y de los pasos
necesarios para reproducirlas, sesión a sesión. Complementa a
[`testing-login.md`](testing-login.md) (guía de login/infraestructura) y a
[`backend.md`](backend.md) (contratos y comportamiento).

## 1. Propósito y uso

- Registrar **qué se probó**, **cómo se probó**, **qué resultado dio** y **qué quedó
  pendiente**, con evidencia verificable (comandos, consultas y salidas resumidas).
- Servir de **runbook**: cualquier persona (o sesión de IA) puede repetir los pasos
  para validar una rama sin redescubrirlos.
- Mantener una **plantilla común** para que las entradas sean comparables entre
  sesiones.

Reglas de mantenimiento:

1. Agregar una entrada por sesión de prueba (o por hito relevante) al final de la
   sección 4, con el próximo ID disponible.
2. No borrar entradas antiguas: si una prueba deja de aplicar, marcar el estado y
   explicar por qué.
3. Nunca registrar secretos ni claves reales (contraseñas, tokens, `AZURE_CLIENT_SECRET`,
   `REGISTRO_CRED_KEY`). Los valores sensibles se referencian por nombre.
4. La evidencia debe poder reproducirse sin acceso a la sesión original.

## 2. Convenciones

- **ID**: `PR-AAAAMMDD-NN` (fecha de la sesión de prueba + correlativo).
- **Estado**: `OK` (verificado), `PARCIAL` (parte verificada), `FALLA`, `BLOQUEADO`
  (falta un prerrequisito), `PENDIENTE` (no ejecutado).
- **Entorno**: rama + commit, y si aplica contenedores/orquestador.
- **Evidencia**: comando ejecutado + resultado resumido (no pegar salidas enormes).
- **Datos de prueba**: usar correos/RUT ficticios y borrarlos al final (registrar la
  limpieza).

## 3. Plantilla de entrada

```markdown
### PR-AAAAMMDD-NN — <título corto>

- **Fecha**: AAAA-MM-DD
- **Rama / commit**: `<rama>` @ `<commit corto>`
- **Alcance**: <qué se probó>
- **Entorno**: <Docker local / Testcontainers / CI / Graph real>
- **Prerrequisitos**: <variables, permisos, servicios>
- **Pasos**: <lista numerada reproducible>
- **Resultado**: <estado> — <detalle>
- **Evidencia**: <comandos + salida resumida>
- **Pendientes / notas**: <si aplica>
```

## 4. Entradas registradas

### PR-20261005-01 — Registro asíncrono de usuarios (E2E con Entra ID)

- **Fecha**: 2026-10-05
- **Rama / commit**: `copilot/explain-microservice-functionality` @ `847dcb4`
- **Alcance**: flujo completo SPA/BFF → `ms-usuarios-auth` → RabbitMQ → Graph +
  consumidores por rol, incluida credencial de un solo uso, `reset-password`, casos
  borde y limpieza.
- **Entorno**: Docker local con MariaDB 11.4, RabbitMQ 4 y tenant real
  (`platformsiga.onmicrosoft.com`).
- **Prerrequisitos**:
  - Graph con permisos de **aplicación** consentidos: `User.Read.All`,
    `User.ReadWrite.All`, `AppRoleAssignment.ReadWrite.All`.
  - Rol de directorio **User Administrator** asignado a la app (para
    `reset-password` y cuentas existentes). Para asignarlo se usó
    `RoleManagement.ReadWrite.Directory` de forma temporal.
  - `.env`: `REGISTRO_ASYNC_ENABLED=true` y `REGISTRO_CRED_KEY` (AES-256, generada
    con `openssl rand -base64 32`).
  - Token ADMIN del navegador (F12 → Network → `/api/bff/...` → `Authorization`).
- **Pasos**:
  1. Preflight: obtener token de aplicación y verificar en el claim `roles` los tres
     permisos de Graph.
  2. Activar el flag y recrear `ms-usuarios-auth`; verificar `healthy`.
  3. `POST /api/bff/v1/admin/registraciones` con un ESTUDIANTE de prueba
     (`prueba.e2e@…`, RUT `21000001-1`); polling del `processId`.
  4. Verificar cuenta en Graph (`accountEnabled`, `forceChangePasswordNextSignIn`),
     app role asignado, `Usuario` `ACTIVO`, invitación `VINCULADA`, perfil en
     `siga_estudiantes_db`, outbox `ENVIADO`, `azureState`/`domainState`
     `COMPLETADO`.
  5. Obtener la credencial una vez; repetir la lectura (debe fallar) y probar
     `reset-password`.
  6. Repetir con un APODERADO (vinculado al estudiante id 1) y ejecutar casos borde:
     RUT inválido (`400`), correo duplicado (`409`), apoderado con estudiante
     inexistente (`400` inmediato por Feign).
  7. Limpieza: `DELETE` de las cuentas de prueba en Entra + filas locales; flag a
     `false`.
- **Resultado**: `OK` — flujo completo verificado (el alta de ESTUDIANTE tardó ~9 s).
  `reset-password` y la reutilización de cuentas existentes solo funcionaron tras
  asignar el rol **User Administrator** a la app (hallazgo registrado en
  `testing-login.md`, sección 2).
- **Evidencia**:
  - 12 colas + DLQ declaradas y consumidores conectados
    (`rabbitmqctl list_consumers`).
  - Flyway `V5`–`V7` aplicadas sobre MariaDB real con `ddl-auto=validate` OK.
  - Consultas a `user_registration_processes`, `user_registration_attempts` y
    `user_registration_outbox` mostrando el ciclo completo.
  - Asignación de app role con reintento acotado por *eventual consistency* de
    Entra (400 recuperado y visible en logs).
- **Pendientes / notas**: el envío de la clave por correo queda reservado
  (`user.credentials.notify` + `REGISTRO_NOTIFY_CREDENTIALS_ENABLED=false`). Los
  secretos de producción (`REGISTRO_ASYNC_ENABLED`, `REGISTRO_CRED_KEY`) deben
  crearse en GitHub Actions (misma clave entre despliegues).

### PR-20261006-02 — CRUD admin, correo automático y normalización de datos

- **Fecha**: 2026-10-06
- **Rama / commit**: `copilot/explain-microservice-functionality` @ `847dcb4`
- **Alcance**: select de clases del año, buscador de alumnos `q`, botón Eliminar con
  offboarding en Entra y reactivación, `usuarios.full_name` (V8) + backfill,
  detalle "Ver", generación automática de correo/nombre, estándar de RUT y nombres
  (migraciones V7/V7/V5/V9) y mejora visual del admin.
- **Entorno**: Docker local; verificación de datos contra MariaDB real.
- **Pasos**:
  1. Verificar en OpenAPI los endpoints admin (`/admin/clases`, `/admin/estudiantes`,
     `/admin/usuarios/{id}`, `DELETE`, `/admin/registraciones`).
  2. En la UI (`/admin/usuarios`): crear estudiante con el select de clases 2026;
     crear apoderado buscando por RUT (`22.126.386-3`), RUT sin puntos y nombre;
     eliminar un usuario y re-registrar el mismo correo (reactivación).
  3. Verificar en BD: `StateUsuario=INACTIVO` tras eliminar, cuenta deshabilitada en
     Graph, y `full_name` poblado en las 65 cuentas.
  4. Ejecutar `tools/normalizar-datos.ps1` para data real previa y verificar
     `0` RUT con puntos y `0` nombres en mayúsculas.
- **Resultado**: `OK` — 6/6 contenedores `healthy`; migraciones `V7`(estudiantes),
  `V7`(docentes), `V5`(apoderados) y `V9`(usuarios) aplicadas; 65/65 `full_name`;
  el caso `del Valle` se corrigió con la regla de partículas siempre en minúscula.
- **Evidencia**:
  - `mvn package` de 12 módulos `BUILD SUCCESS`; `tsc` + 11 tests + build de
    producción del frontend OK.
  - Consultas de verificación: RUT con puntos = 0, nombres en mayúsculas = 0,
    `usuarios.full_name` 65/65.
- **Pendientes / notas**: la propagación de la desactivación a los perfiles de
  dominio por evento (`user.account.disabled/enabled`) queda como iteración futura;
  "estudiante requiere apoderado" sigue fuera de alcance.

### PR-20261006-03 — Fix de fechas en Feign (asistencias del docente)

- **Fecha**: 2026-10-06
- **Rama / commit**: `copilot/explain-microservice-functionality` @ `847dcb4`
- **Alcance**: `GET /api/bff/v1/asistencias/asignatura/{id}?fecha=…` devolvía `400`
  porque OpenFeign serializaba `from`/`to` como fecha corta localizada (`6/10/26`).
- **Entorno**: Docker local (portal docente).
- **Prerrequisitos**: stack con `bff-web` y `ms-asistencias`.
- **Pasos**:
  1. Reproducir el error en la grilla de asistencias y confirmar en logs de
     `ms-asistencias` el mensaje `El parametro 'from' tiene un formato invalido`.
  2. Corregir con `SharedFeignFormatConfig` en `core-share` (registra
     `yyyy-MM-dd`, `HH:mm:ss` y `yyyy-MM-dd HH:mm:ss` en el conversion service de
     Feign) y cubrir con `SharedFeignFormatConfigTest`.
  3. Reconstruir `bff-web` y verificar que la clase queda en el fat jar.
  4. Recargar la página del docente y confirmar `200` (o "sin registros") y la
     ausencia del error en logs.
- **Resultado**: `PARCIAL` — fix compilado, test en verde y desplegado; la
  confirmación en vivo quedó pendiente de la recarga del usuario al cierre de la
  sesión.
- **Evidencia**: `SharedFeignFormatConfigTest` en verde; clase presente en
  `core-share` y en el fat jar de `bff-web`; contenedor recreado `healthy`.
- **Pendientes / notas**: no lo causaban los cambios de normalización; era un bug
  preexistente y `AsistenciaClient` es el único cliente Feign que enviaba fechas.

## 5. Runbook reproducible

### 5.1 Prerrequisitos

- Docker Desktop con el stack de SIGA; idealmente **detener otras stacks**
  (p. ej. `pasteleria-project`) para evitar caídas del engine por carga.
- `.env` completo (MARIADB, DB, AZURE, RABBITMQ y `REGISTRO_*`).
- Para flujos reales: permisos de Graph de la sección 2 de
  [`testing-login.md`](testing-login.md) y token ADMIN del navegador.

Generar la clave de credenciales:

```bash
openssl rand -base64 32
```

### 5.2 Pruebas automáticas (mismas flags del CI)

Backend (desde la raíz del repo):

```bash
mvn -f apps/backend/pom.xml -B -ntp package \
  -Dtest='!*ApplicationTests' -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtypescript.generator.skip=true
```

Frontend (desde `apps/frontend`):

```bash
npx tsc -p tsconfig.app.json --noEmit
npm test -- --watch=false
npm run build -- --configuration production
```

### 5.3 Levantar y reconstruir

- Stack completo: `docker compose up -d --build` y `docker compose ps`.
- Tras cambios acotados, reconstruir **de a uno** para no saturar Docker Desktop:
  `ms-usuarios-auth` → `ms-estudiantes` → `ms-docentes` → `ms-apoderados` →
  `bff-web` → `frontend` (`docker compose up -d --build <servicio>`).
- Verificar salud: `docker compose ps` (todos `healthy`).

### 5.4 Registro asíncrono (si se va a probar el flujo real)

1. `.env`: `REGISTRO_ASYNC_ENABLED=true` y `REGISTRO_CRED_KEY=<clave>`; recrear
   `ms-usuarios-auth`.
2. Iniciar el alta desde la UI o con `POST /api/bff/v1/admin/registraciones`
   (token ADMIN) y hacer polling del `processId` con
   `GET /api/bff/v1/admin/registraciones/{processId}`.
3. Credencial: `GET .../credencial` una sola vez (`409`/`410` según estado) y
   `POST /admin/usuarios/{id}/reset-password` como respaldo.
4. Casos borde: RUT inválido → `400`; correo duplicado → `409`; apoderado con
   estudiante inexistente → `400` inmediato.
5. Limpieza: eliminar cuentas de prueba en Entra (o deshabilitarlas) y sus filas
   locales; volver `REGISTRO_ASYNC_ENABLED=false`.

### 5.5 Verificación de datos (SQL)

```sql
-- Estado de procesos, intentos y outbox (siga_usuarios_db)
SELECT process_id, state, azure_state, domain_state, error_message
  FROM user_registration_processes ORDER BY created_at DESC LIMIT 5;
SELECT process_id, step_type, success, message
  FROM user_registration_attempts ORDER BY id DESC LIMIT 10;
SELECT event_id, routing_key, state, attempts, last_error
  FROM user_registration_outbox ORDER BY id DESC LIMIT 5;

-- Estandar de datos
SELECT COUNT(*) FROM estudiantes WHERE rut LIKE '%.%';   -- esperado: 0
SELECT COUNT(*) FROM usuarios WHERE full_name IS NULL;   -- esperado: 0
```

### 5.6 Verificación de mensajería

```bash
docker exec rabbitmq rabbitmqctl list_queues name messages
docker exec rabbitmq rabbitmqctl list_consumers
```

Colas esperadas: `user.azure.sync.queue`, `ms.estudiantes.queue`,
`ms.docentes.queue`, `ms.apoderados.queue`, `user.registration.status.queue`,
`user.credentials.notify.queue` y una DLQ por cola. UI:
`http://localhost:15672`.

### 5.7 Normalización de datos previos

- Los seeds se normalizan solos con las migraciones (`V7`/`V7`/`V5`/`V9`).
- Para filas reales creadas antes del cambio (si existen), ejecutar una vez por
  entorno:

```powershell
powershell -File tools/normalizar-datos.ps1
```

### 5.8 Limpieza de cuentas de prueba

- Entra ID: deshabilitar o eliminar las cuentas ficticias (si se eliminan, el UPN
  queda reservado ~30 días; para reutilizar el mismo correo conviene deshabilitar).
- BD: borrar filas de `usuarios`, perfiles, procesos, intentos, outbox y eventos
  procesados asociados a los correos de prueba.

## 6. Limitaciones conocidas del entorno

- **Testcontainers en Windows**: no logra comunicarse con el *named pipe* de Docker
  Desktop moderno; los IT se omiten o se validan contra el MariaDB del stack real.
  En CI Linux corren normalmente.
- **Docker Desktop** puede devolver `500` y caerse si se reconstruyen 5 imágenes
  Java + frontend en paralelo con otra stack corriendo; reconstruir de a uno y
  detener stacks ajenas.
- **UTF-8 en consola**: al escribir datos con acentos por `docker exec`, usar
  Base64/UTF-8 explícito para evitar corrupción.

## 7. Inventario de pruebas automáticas

| Módulo | Suites relevantes |
| --- | --- |
| `core-share` | `ValidatorsTest`, `EnumsJsonTest`, `RutNormalizerTest`, `NombrePropioTest`, `SharedFeignFormatConfigTest`, `GlobalExceptionHandlerTest` |
| `ms-usuarios-auth` | `UsuarioServiceTest`, `InvitacionVinculacionServiceTest`, `AsyncUserRegistrationServiceTest`, `CredentialCipherTest`, `EmailInstitucionalGeneratorTest`, `MigracionesTest`, `MigracionesRegistroAsyncTest` |
| `bff-web` | `AdminBffServiceTest`, `PerfilEstudianteServiceTest`, `ApoderadoPupiloServiceTest`, `DocenteBffServiceTest`, `NotaBffServiceTest`, `EvaluacionBffServiceTest`, `AsistenciaBffServiceTest` |
| Dominios | `EstudianteServiceTest` + `BusquedaTextoTest`, `DocenteServiceTest`, `ApoderadoServiceTest`, `ClaseServiceTest`, `NotaServiceTest`, `EvaluacionServiceTest`, `AsistenciaServiceTest` y `PreAuthorizeExpressionsTest` |
| Frontend | specs Vitest de `app`, `docente-notas.service` y `asistencia.service` |

Los `*ApplicationTests` (contextLoads) se excluyen en CI porque requieren base de
datos y Azure AD.

## 8. Checklist de cierre de sesión

- [ ] Pruebas automáticas backend/frontend ejecutadas y en verde.
- [ ] Flujo funcional probado (UI o API) y evidencia registrada.
- [ ] Migraciones aplicadas en las BD reales y `ddl-auto=validate` OK.
- [ ] Colas/consumidores verificados (si aplica).
- [ ] Datos de prueba limpiados (Entra + BD).
- [ ] Flags devueltos a su estado acordado (`REGISTRO_ASYNC_ENABLED`).
- [ ] Documentación actualizada y entrada nueva en este registro.
