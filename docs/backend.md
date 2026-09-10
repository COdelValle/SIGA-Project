# Documentacion del backend

## 1. Proposito

El backend de SIGA esta organizado como un proyecto Maven multi-modulo basado en Spring Boot. Su objetivo es separar las responsabilidades del dominio academico en servicios independientes, exponer APIs protegidas y centralizar los contratos compartidos.

La estructura ya existe, pero la implementacion funcional aun es parcial. Hay modulos con entidades, repositorios y servicios iniciales, mientras que otros solo tienen su aplicacion y configuracion base.

## 2. Ubicacion y tecnologias

- Ubicacion: `apps/backend`
- Java 21
- Spring Boot 3.5.0
- Maven
- Spring Security y OAuth2 Resource Server con JWT
- Spring Cloud OpenFeign para comunicacion entre servicios
- Spring Data JPA e Hibernate
- MapStruct y Lombok
- Springdoc OpenAPI y Scalar
- MariaDB como base de datos prevista

El `pom.xml` padre centraliza versiones, dependencias y modulos. Los modulos actualmente declarados son `core-share`, `bff-web`, `ms-usuarios-auth`, `ms-estudiantes`, `ms-asignaturas` y `ms-notas`.

## 3. Componentes del backend

### 3.1 BFF Web

Ubicacion: `apps/backend/bff-web`

Es la puerta de entrada pensada para el frontend. Debe ocultar la topologia interna, coordinar solicitudes y entregar respuestas orientadas a las necesidades de la interfaz.

Responsabilidades previstas:

- Exponer endpoints consumibles por Angular.
- Validar el token y propagar el contexto de seguridad.
- Orquestar llamadas a microservicios.
- Unificar respuestas y errores para el frontend.
- Evitar que el navegador dependa de las direcciones internas de cada servicio.

Actualmente contiene la aplicacion Spring Boot, configuracion de seguridad y un DTO provisional. La orquestacion y los endpoints funcionales siguen pendientes.

### 3.2 Servicio de usuarios y autenticacion

Ubicacion: `apps/backend/ms-usuarios-auth`

Responsabilidad de administrar usuarios, roles y estado de las cuentas, ademas de integrarse con la autenticacion basada en JWT.

La base actual incluye:

- Entidad `Usuario`.
- `UsuarioRepository`.
- `UsuarioService`.
- `UsuarioController`.
- Mapper y especificaciones de consulta.
- DTO de registro, actualizacion y respuesta en `core-share`.

Pendientes principales:

- Definir completamente el contrato de autenticacion.
- Resolver el modelo final de identidad entre Azure AD y SIGA.
- Completar autorizacion por roles y permisos.
- Agregar pruebas y persistencia verificada.

### 3.3 Servicio de estudiantes

Ubicacion: `apps/backend/ms-estudiantes`

Responsabilidad de administrar la ficha academica y personal de los estudiantes.

La base actual incluye:

- Entidad `Estudiante`.
- `EstudianteRepository`.
- `EstudianteService`.
- Mapper de estudiante.
- DTO de registro, actualizacion y respuesta compartidos.
- Configuracion para base de datos y seguridad.

El servicio debera evolucionar hacia operaciones REST completas, validaciones, filtros, matricula y relaciones academicas. Esas capacidades aun no estan terminadas.

### 3.4 Servicio de asignaturas

Ubicacion: `apps/backend/ms-asignaturas`

Responsabilidad prevista de gestionar asignaturas, cursos, docentes y sus relaciones academicas.

Actualmente existe la aplicacion Spring Boot y una configuracion de seguridad. El dominio, las entidades, los repositorios, los servicios y los controladores funcionales deben implementarse.

### 3.5 Servicio de notas

Ubicacion: `apps/backend/ms-notas`

Responsabilidad prevista de registrar y consultar calificaciones relacionadas con estudiantes y asignaturas.

Actualmente existe la aplicacion y la configuracion base del modulo. Quedan pendientes las entidades, reglas de rango y periodo, repositorios, servicios, endpoints y pruebas.

### 3.6 Biblioteca compartida

Ubicacion: `apps/backend/libs/core-share`

Centraliza elementos reutilizables sin convertir los microservicios en un unico modulo de negocio.

Incluye actualmente:

- DTO de usuarios, estudiantes, asignaturas y notas.
- Enumeraciones de roles y estado de usuario.
- Validadores de RUT, telefono y calificacion chilena.
- Configuracion de seguridad compartida.
- Utilidades de seguridad.
- Excepciones de negocio y respuestas de error.
- Configuracion compartida de OpenAPI.

La libreria debe mantenerse enfocada en contratos, validaciones y componentes transversales. Las entidades y reglas especificas deben permanecer en su microservicio.

## 4. Seguridad y contratos

La seguridad esta preparada para OAuth2/JWT. Las rutas de documentacion pueden exponerse publicamente, mientras que el resto de los endpoints debe requerir autenticacion.

La documentacion OpenAPI se contempla mediante:

- `/v3/api-docs`
- `/docs/swagger`
- `/docs/scalar`

Los contratos deben definirse primero en cada servicio y luego reflejarse en los DTO compartidos y en los modelos TypeScript del frontend.

## 5. Datos y configuracion

Los servicios esperan variables de entorno para conexion a MariaDB, credenciales y direcciones internas. Entre las variables previstas se encuentran `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASS`, `AZURE_TENANT_ID`, `MS_ESTUDIANTES_URL`, `MS_ASIGNATURAS_URL` y `MS_NOTAS_URL`.

Docker Compose contiene una base inicial para la red, el volumen de MariaDB y `ms-estudiantes`. La orquestacion completa de todos los servicios y la definicion de salud de cada uno estan pendientes.

## 6. Estado y orden recomendado

1. Definir entidades y reglas del dominio.
2. Completar repositorios, servicios y controladores de cada microservicio.
3. Definir contratos OpenAPI y respuestas de error.
4. Implementar autenticacion y autorizacion por rol.
5. Implementar la orquestacion del BFF.
6. Integrar MariaDB y migraciones de esquema.
7. Añadir pruebas unitarias, de integracion y de contrato.
8. Completar Docker Compose y observabilidad.

El backend tiene una base tecnica aprovechable, pero no debe considerarse una API terminada hasta que esos flujos esten implementados y probados.
