package cl.siga.msasignaturas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Aplica las migraciones Flyway sobre un MariaDB real y valida el catálogo,
 * la malla, la unicidad de dictaciones y el soft delete de horarios.
 */
@Testcontainers(disabledWithoutDocker = true)
class MigracionesTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
            .withDatabaseName("siga_asignaturas_db")
            .withUsername("siga")
            .withPassword(UUID.randomUUID().toString());

    @Test
    void aplicaMigracionesYValidaCatalogoMallaYHorarios() throws Exception {
        Flyway.configure()
                .dataSource(MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword());
             Statement statement = connection.createStatement()) {

            var catalogo = statement.executeQuery("SELECT COUNT(*) FROM asignaturas");
            assertTrue(catalogo.next());
            assertEquals(22, catalogo.getInt(1));

            var dictaciones = statement.executeQuery("SELECT COUNT(*) FROM cursos_asignaturas");
            assertTrue(dictaciones.next());
            assertEquals(143, dictaciones.getInt(1));

            var malla = statement.executeQuery("SELECT COUNT(*) FROM malla_curricular");
            assertTrue(malla.next());
            assertTrue(malla.getInt(1) >= 100);

            var orientacion = statement.executeQuery(
                    "SELECT calificable FROM asignaturas WHERE nombre = 'Orientación'");
            assertTrue(orientacion.next());
            assertFalse(orientacion.getBoolean(1));

            // nombre es UNIQUE: duplicar el catálogo (seed) debe fallar
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO asignaturas (nombre, descripcion, area, calificable, active) "
                            + "VALUES ('Matemática', 'duplicada', 'MATEMATICAS', TRUE, TRUE)"));

            // horarios.active tiene default TRUE (soft delete)
            statement.executeUpdate(
                    "INSERT INTO horarios (dia, horario_entrada, horario_salida, ubicacion, curso_asignatura_id) "
                            + "VALUES ('LUNES', '07:00:00', '07:45:00', 'SALA TEST', 1)");

            var active = statement.executeQuery("SELECT active FROM horarios WHERE ubicacion = 'SALA TEST'");
            assertTrue(active.next());
            assertTrue(active.getBoolean(1));
        }
    }
}
