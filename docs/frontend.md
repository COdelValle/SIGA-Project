# Documentacion del frontend

## 1. Proposito

El frontend de SIGA sera la interfaz web para los usuarios del sistema de gestion academica. Su responsabilidad sera presentar los flujos de autenticacion, consulta y administracion, y comunicarse con el backend a traves del BFF Web.

Actualmente el frontend es una base inicial generada con Angular CLI. La aplicacion muestra la pantalla de bienvenida de Angular y aun no contiene los modulos funcionales del sistema.

## 2. Ubicacion y tecnologias

- Ubicacion: `apps/frontend`
- Angular 22.1
- TypeScript 6
- RxJS 7.8
- Angular Router
- Formularios de Angular
- Vitest para pruebas
- Node.js y npm

Los comandos principales se ejecutan desde `apps/frontend`:

```bash
npm install
npm start
npm run build
npm test
```

La aplicacion de desarrollo queda disponible, por defecto, en `http://localhost:4200/`.

## 3. Estructura actual

- `src/main.ts`: punto de entrada de la aplicacion.
- `src/index.html`: documento HTML base.
- `src/styles.css`: estilos globales.
- `src/app/app.ts`: componente raiz actual.
- `src/app/app.html`: plantilla inicial de Angular.
- `src/app/app.css`: estilos del componente raiz.
- `src/app/app.config.ts`: configuracion de la aplicacion.
- `src/app/app.routes.ts`: rutas, actualmente sin flujos de negocio implementados.
- `src/app/app.spec.ts`: prueba inicial del componente raiz.
- `src/types`: espacio reservado para tipos compartidos del frontend.

## 4. Modulos funcionales previstos

### Autenticacion y sesion

Permitira iniciar y cerrar sesion, conservar el estado del usuario, proteger rutas y adjuntar el token JWT a las solicitudes autorizadas. La autenticacion debera integrarse con el mecanismo configurado en `ms-usuarios-auth` y Azure AD.

### Inicio y navegacion por rol

Funcionara como punto de entrada despues del login. Mostrara accesos segun el rol del usuario, por ejemplo administrador, docente o usuario administrativo.

### Usuarios y roles

Permitira consultar, registrar, actualizar y desactivar usuarios cuando el rol tenga permisos para hacerlo. Las operaciones se consumiran desde el BFF y no directamente desde cada microservicio.

### Estudiantes

Permitira consultar y administrar la ficha del estudiante, incluyendo sus datos personales, identificacion, informacion academica y relaciones con cursos o asignaturas.

### Asignaturas y docentes

Permitira consultar asignaturas, cursos, docentes y las relaciones entre ellos. La interfaz debera contemplar formularios de mantenimiento y vistas de consulta.

### Notas

Permitira registrar, editar y consultar calificaciones por estudiante y asignatura, con validaciones de rango y permisos segun el perfil del usuario.

### Reportes y consultas

Se podran agregar vistas para resumenes academicos, listados filtrables y exportacion. Este modulo depende de que los contratos del backend esten definidos.

## 5. Capas que se deben implementar

- Componentes y paginas para cada flujo.
- Servicios HTTP para comunicarse con el BFF.
- Interceptores para JWT, errores y estados de carga.
- Guards para autenticacion y autorizacion por rol.
- Modelos TypeScript alineados con los DTO del backend.
- Formularios reactivos y validaciones.
- Manejo de errores y mensajes para el usuario.
- Pruebas unitarias de componentes, servicios y guards.

## 6. Estado actual y trabajo pendiente

| Area | Situacion |
| --- | --- |
| Bootstrap de Angular | Disponible |
| Ruteo de negocio | Pendiente |
| Login y sesion | Pendiente |
| Componentes academicos | Pendiente |
| Servicios HTTP | Pendiente |
| Integracion con BFF | Pendiente |
| Control de roles | Pendiente |
| Pruebas funcionales | Pendiente |

El siguiente paso recomendado es definir primero el flujo de autenticacion y el layout principal. Luego se pueden implementar los modulos de estudiantes, asignaturas y notas sobre contratos de API estables.
