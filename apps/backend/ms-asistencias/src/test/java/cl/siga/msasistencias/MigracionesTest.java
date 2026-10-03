package cl.siga.msasistencias;

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
 * Aplica las migraciones Flyway sobre un MariaDB real y valida el unique
 * (estudiante, asignatura, fecha) y el soft delete (active con default TRUE).
 */
@Testcontainers(disabledWithoutDocker = true)
class MigracionesTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
            .withDatabaseName("siga_asistencias_db")
            .withUsername("siga")
            .withPassword(UUID.randomUUID().toString());

    @Test
    void aplicaMigracionesYValidaUniqueYSoftDelete() throws Exception {
        Flyway.configure()
                .dataSource(MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword());
             Statement statement = connection.createStatement()) {

            var seed = statement.executeQuery("SELECT COUNT(*) FROM asistencias");
            assertTrue(seed.next());
            assertTrue(seed.getInt(1) >= 2, "deberia haber asistencias de seed");

            // Fecha fuera del rango del seed (03-08 a 02-10-2026) para probar el unique.
            statement.executeUpdate(
                    "INSERT INTO asistencias (id_estudiante, id_asignatura, fecha, estado, justificacion) "
                            + "VALUES (1, 1, '2026-10-05', 'PRESENTE', 'NO_APLICA')");
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO asistencias (id_estudiante, id_asignatura, fecha, estado, justificacion) "
                            + "VALUES (1, 1, '2026-10-05', 'AUSENTE', 'PENDIENTE')"));

            // active tiene default TRUE (soft delete).
            var activo = statement.executeQuery(
                    "SELECT active FROM asistencias WHERE id_estudiante = 1 AND id_asignatura = 1 AND fecha = '2026-10-05'");
            assertTrue(activo.next());
            assertTrue(activo.getBoolean(1));
        }
    }
}
