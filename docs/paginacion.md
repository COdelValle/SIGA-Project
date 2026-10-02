# Paginacion de busquedas

Todos los endpoints `GET /search` de los microservicios devuelven una pagina
(`Page<T>`, contrato de Spring Data) en lugar de una lista completa. Este
documento describe el contrato, los endpoints afectados y como consumirlo desde
el backend y el frontend.

## 1. Resumen

| Aspecto | Valor |
| --- | --- |
| Endpoints | Los 9 `GET /search` (ver tabla en la seccion 4) |
| Parametros | `page` (0-indexado), `size`, `sort` |
| Tamano por defecto | `20` |
| Tamano maximo | `100` (si se pide mas, se recorta) |
| Orden por defecto | `id,asc` |
| Respuesta | `Page<T>` con `content` y metadatos |
| Configuracion | `apps/backend/libs/core-share/src/main/resources/common-properties.yaml` |

## 2. Parametros de la solicitud

| Parametro | Tipo | Default | Descripcion |
| --- | --- | --- | --- |
| `page` | entero >= 0 | `0` | Numero de pagina (0 es la primera). |
| `size` | entero >= 1 | `20` | Cantidad de elementos por pagina (maximo `100`). |
| `sort` | texto | `id,asc` | Campo y direccion: `campo,asc` o `campo,desc`. Se puede repetir para orden multiple. |

Ejemplos:

```
GET /api/v1/usuarios/search?page=0&size=20
GET /api/v1/estudiantes/search?page=1&size=50&sort=firstSurname,asc
GET /api/v1/notas/search?idEstudiante=1&page=0&size=20&sort=score,desc
```

> Los nombres de `sort` son las propiedades de la entidad (por ejemplo `id`,
> `firstName`, `score`, `fechaContratacion`), no los titulos de la interfaz.

## 3. Respuesta `Page<T>`

```json
{
  "content": [
    { "id": 1, "email": "camila.soto@platformsiga.onmicrosoft.com", "rol": "ESTUDIANTE", "state": "ACTIVO" }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20,
  "numberOfElements": 20,
  "first": true,
  "last": false,
  "empty": false
}
```

| Campo | Descripcion |
| --- | --- |
| `content` | Elementos de la pagina actual (los DTOs de siempre). |
| `totalElements` | Total de elementos que cumplen los filtros. |
| `totalPages` | Total de paginas. |
| `number` | Pagina actual (0-indexada). |
| `size` | Tamano solicitado (recortado al maximo). |
| `numberOfElements` | Cantidad de elementos en `content`. |
| `first` / `last` | Indica si es la primera o ultima pagina. |
| `empty` | `true` si `content` esta vacio. |

## 4. Endpoints paginados

| Servicio | Puerto | Ruta | Filtros |
| --- | --- | --- | --- |
| `ms-usuarios-auth` | 8081 | `GET /api/v1/usuarios/search` | `email`, `rol`, `state` |
| `ms-estudiantes` | 8083 | `GET /api/v1/estudiantes/search` | `rut`, nombres, `from`, `to`, `state` |
| `ms-asignaturas` | 8086 | `GET /api/v1/asignaturas/search` | `name`, `tipo`, `semestre`, `area`, `idDocente`, `idClase`, `conCupoDisponible` |
| `ms-asignaturas` | 8086 | `GET /api/v1/inscripciones/search` | `idAlumno`, `idAsignatura`, `estados` |
| `ms-notas` | 8089 | `GET /api/v1/notas/search` | `idEstudiante`, `idEvaluacion`, `lessThanScore`, `greaterThanScore` |
| `ms-docentes` | 8085 | `GET /api/v1/docentes/search` | `rut`, nombres, `from`, `to`, `area` |
| `ms-apoderados` | 8084 | `GET /api/v1/apoderados/search` | `rut`, nombres, `idEstudiante` |
| `ms-clases` | 8087 | `GET /api/v1/clases/search` | `nivel`, `letra`, `anioAcademico`, `idDocenteJefe` |
| `ms-evaluaciones` | 8088 | `GET /api/v1/evaluaciones/search` | `nombre`, `tipo`, `idAsignatura` |

Los demas endpoints (`GET /{id}`, `exists`, `POST`, `PUT`, `DELETE`) no cambian.

## 5. Ejemplo completo

```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8081/api/v1/usuarios/search?rol=DOCENTE&page=0&size=2&sort=email,asc"
```

```json
{
  "content": [
    { "id": "...", "email": "alejandro.silva@platformsiga.onmicrosoft.com", "rol": "DOCENTE", "state": "ACTIVO" },
    { "id": "...", "email": "augusto.figueroa@platformsiga.onmicrosoft.com", "rol": "DOCENTE", "state": "ACTIVO" }
  ],
  "totalElements": 14,
  "totalPages": 7,
  "number": 0,
  "size": 2,
  "numberOfElements": 2,
  "first": true,
  "last": false,
  "empty": false
}
```

## 6. Errores y limites

- `page` o `size` **no numericos** producen **400** con el formato
  `ErrorResponseDTO` (`{ timestamp, status, error, message, details }`).
- `page` negativo se ajusta a `0`; `size` mayor al maximo se **recorta a 100** y
  un `size` menor a 1 usa el valor por defecto. No son errores.
- Un `sort` con un campo inexistente responde **400** indicando el campo invalido.

## 7. Configuracion

`common-properties.yaml` (compartido por todos los servicios) fija los valores:

```yaml
spring:
  data:
    web:
      pageable:
        default-page-size: 20
        max-page-size: 100
```

Cada controlador declara ademas el orden por defecto:

```java
@PageableDefault(size = 20, sort = "id") Pageable pageable
```

## 8. Backend: como implementar una busqueda paginada

```java
@GetMapping("/search")
@PreAuthorize("hasAuthority('SCOPE_recurso:read')")
public ResponseEntity<Page<RecursoResponseDTO>> search(
        @RequestParam(required = false) String filtro,
        @PageableDefault(size = 20, sort = "id") Pageable pageable) {
    return ResponseEntity.ok(service.search(filtro, pageable));
}

// En el servicio:
return repository.findAll(spec, pageable).map(mapper::toResponseDto);
```

Los repositorios extienden `JpaSpecificationExecutor`, por lo que
`findAll(Specification, Pageable)` devuelve `Page<Entidad>` directamente.

## 9. Frontend: como consumirlo

Modelo y utilidades en `@siga/core` (`libs/core/src/lib/models/page.model.ts`):

```ts
import { Page, PageQuery, pageQueryParams, toPage } from '@siga/core';

// Peticion HTTP real (cuando el recurso este conectado al BFF)
this.http.get<Page<Usuario>>(`${base}/usuarios/search`, {
  params: { ...filtros, ...pageQueryParams({ page: 0, size: 20, sort: 'email,asc' }) },
});

// Mientras las vistas usan mocks, se pagina en cliente:
const pagina = toPage(filtrados, page - 1, pageSize);
```

UI: `siga-paginador` (en `@siga/shared-ui`) muestra "Mostrando X–Y de Z" y los
botones de navegacion; emite la pagina seleccionada (1-indexada). Vistas que ya
lo usan: `admin/usuarios`, `admin/asignaturas` y `academico/asistencia-historial`.

Al cambiar un filtro o la busqueda, la vista debe volver a la pagina 1.

## 10. Migracion desde el contrato anterior

- Antes los `GET /search` devolvian `List<T>`; ahora devuelven `Page<T>`.
- Los consumidores deben leer `content` (y opcionalmente `totalElements`,
  `totalPages`) en lugar del arreglo directo.
- El frontend ya usa `Page<T>`/`toPage()`; al conectar los endpoints reales solo
  hay que enviar `page`, `size` y `sort` con `pageQueryParams()`.
