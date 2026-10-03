package cl.siga.msasignaturas;

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
 * Aplica las migraciones Flyway sobre un MariaDB real y valida la unicidad de
 * asignaturas y el soft delete de horarios (columna active con default TRUE).
 */
@Testcontainers(disabledWithoutDocker = true)
class MigracionesTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
            .withDatabaseName("siga_asignaturas_db")
            .withUsername("siga")
            .withPassword(UUID.randomUUID().toString());

    @Test
    void aplicaMigracionesYValidaUnicidadYHorarios() throws Exception {
        Flyway.configure()
                .dataSource(MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword());
             Statement statement = connection.createStatement()) {

            // name es UNIQUE: duplicar MATEMATICA (seed) debe fallar
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO asignaturas (tipo_asignatura, name, description, semestre, area, id_docente, active, id_clase, cupo_maximo) "
                            + "VALUES ('BASICA', 'MATEMATICA', 'duplicada', 'SEMESTRE_1', 'MATEMATICAS', 1, TRUE, 1, NULL)"));

            // horarios.active tiene default TRUE (soft delete)
            statement.executeUpdate(
                    "INSERT INTO horarios (dia, horario_entrada, horario_salida, ubicacion, asignatura_id) "
                            + "VALUES ('LUNES', '07:00:00', '07:45:00', 'SALA TEST', 1)");

            var active = statement.executeQuery("SELECT active FROM horarios WHERE ubicacion = 'SALA TEST'");
            assertTrue(active.next());
            assertTrue(active.getBoolean(1));
        }
    }
}
