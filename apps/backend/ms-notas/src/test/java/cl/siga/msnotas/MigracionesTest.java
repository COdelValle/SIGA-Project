package cl.siga.msnotas;

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
 * (estudiante, evaluacion) de la V5 que blinda el auto-guardado del docente.
 * El unique se mantiene incluso con soft delete: la capa de servicio reactiva
 * la fila inactiva en lugar de insertar un duplicado.
 */
@Testcontainers(disabledWithoutDocker = true)
class MigracionesTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
            .withDatabaseName("siga_notas_db")
            .withUsername("siga")
            .withPassword(UUID.randomUUID().toString());

    @Test
    void aplicaMigracionesYValidaUniqueDeNotas() throws Exception {
        Flyway.configure()
                .dataSource(MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword());
             Statement statement = connection.createStatement()) {

            var seed = statement.executeQuery("SELECT COUNT(*) FROM notas");
            assertTrue(seed.next());
            assertTrue(seed.getInt(1) >= 3, "deberia haber notas de seed");

            // Par fuera del seed y duplicado exacto: el unique lo rechaza.
            statement.executeUpdate(
                    "INSERT INTO notas (id_estudiante, id_evaluacion, score, active) VALUES (999, 9999, 6.0, TRUE)");
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO notas (id_estudiante, id_evaluacion, score, active) VALUES (999, 9999, 5.5, TRUE)"));

            // El unique aplica tambien a filas inactivas (la re-creacion se
            // resuelve reactivando la misma fila desde NotaService).
            statement.executeUpdate(
                    "UPDATE notas SET active = FALSE WHERE id_estudiante = 999 AND id_evaluacion = 9999");
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO notas (id_estudiante, id_evaluacion, score, active) VALUES (999, 9999, 7.0, TRUE)"));

            var activo = statement.executeQuery(
                    "SELECT active FROM notas WHERE id_estudiante = 999 AND id_evaluacion = 9999");
            assertTrue(activo.next());
            assertTrue(!activo.getBoolean(1));
        }
    }
}
