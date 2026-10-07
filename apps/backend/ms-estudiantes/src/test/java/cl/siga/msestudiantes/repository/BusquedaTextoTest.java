package cl.siga.msestudiantes.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import cl.siga.coreshare.dto.estudiante.enums.State;
import cl.siga.msestudiantes.model.entity.Estudiante;
import cl.siga.msestudiantes.model.specifications.EstudianteSpecifications;

/**
 * Valida el buscador de alumnos del selector (RUT con/sin puntos y nombres por
 * palabras) sobre MariaDB real.
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class BusquedaTextoTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
            .withDatabaseName("siga_estudiantes_test")
            .withUsername("siga")
            .withPassword(UUID.randomUUID().toString());

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
    }

    @Autowired
    private EstudianteRepository repository;

    private Estudiante estudiante(String oid, String rut, String nombre, String apellido) {
        return Estudiante.builder()
                .idUsuario(oid)
                .rut(rut)
                .firstName(nombre)
                .firstSurname(apellido)
                .birthDate(LocalDate.of(2012, 1, 1))
                .state(State.REGISTRADO)
                .build();
    }

    @Test
    void buscaPorRutConPuntosSinPuntosYPorNombreApellido() {
        String oidCatalina = UUID.randomUUID().toString();
        String oidOtro = UUID.randomUUID().toString();
        repository.save(estudiante(oidCatalina, "22126386-3", "CATALINA", "ORMEÑO"));
        repository.save(estudiante(oidOtro, "21000001-1", "OTRO", "ALUMNO"));

        var porRutConPuntos = repository.findAll(
                EstudianteSpecifications.isActive()
                        .and(EstudianteSpecifications.hasTextoLibre("22.126.386-3")),
                PageRequest.of(0, 10));
        assertEquals(1, porRutConPuntos.getTotalElements());
        assertEquals(oidCatalina, porRutConPuntos.getContent().get(0).getIdUsuario());

        var porRutSinPuntos = repository.findAll(
                EstudianteSpecifications.hasTextoLibre("22126386"),
                PageRequest.of(0, 10));
        assertEquals(1, porRutSinPuntos.getTotalElements());

        var porNombreYApellido = repository.findAll(
                EstudianteSpecifications.hasTextoLibre("catalina ormeño"),
                PageRequest.of(0, 10));
        assertEquals(1, porNombreYApellido.getTotalElements());
    }
}
