# `ms-docentes`

**Módulo:** `apps/backend/ms-docentes` · **Puerto:** 8085 · **BD:** `siga_docentes_db`.

## Responsabilidad y datos

Es dueño de perfiles de docentes y certificados asociados (`Docente`, `Certificado`), datos laborales/área y estado activo. Persistencia JPA y migraciones Flyway; la identidad externa se representa por `idUsuario` sin compartir la base de usuarios.

Fuentes principales: `controller/DocenteController.java`, `controller/CertificadoController.java`, `service/DocenteService.java`, `service/CertificadoService.java`, `repository/DocenteRepository.java`, `repository/CertificadoRepository.java` y `messaging/DocenteRegistrationListener.java`.

## Dependencias declaradas

[`pom.xml`](../../apps/backend/ms-docentes/pom.xml): Web, Validation, AMQP, JPA/MariaDB runtime, Flyway, Security/OAuth2 Resource Server, Springdoc, Actuator, MapStruct y `core-share`. No declara Feign directo en este POM.

## Patrones y dependencias

- Separación controller/service/repository; certificados como subrecurso del docente.
- Autorización por rol/scope y ownership para autoservicio del docente.
- `DocenteRegistrationListener` consume evento del alta de usuarios; es idempotente por `idUsuario`, crea/reactiva perfil y publica estado de etapa.
- No tiene dependencia HTTP runtime a otro microservicio en su POM actual. Su integración con usuarios es asíncrona por Rabbit; sus datos se consultan a través del BFF y `ms-asignaturas`/`ms-clases` mediante sus propios clientes Feign.

## API y operación

`/api/v1/docentes`: CRUD, búsqueda, existencia, perfil por `idUsuario` y `/certificados`. Cada consulta del dominio la protege JWT/roles/scopes. Tiene DB propia/Flyway y participa en la topología de registro. `src/test/java` contiene pruebas del dominio; alta asíncrona requiere Rabbit y cuenta Graph ya aprovisionada.
