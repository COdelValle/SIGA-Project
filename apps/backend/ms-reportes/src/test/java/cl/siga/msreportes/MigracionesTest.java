package cl.siga.msreportes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Aplica todas las migraciones sobre MariaDB y verifica el esquema requerido
 * por las proyecciones de reportes.
 */
@Testcontainers(disabledWithoutDocker = true)
class MigracionesTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
            .withDatabaseName("siga_reportes_db")
            .withUsername("siga")
            .withPassword(UUID.randomUUID().toString());

    @Test
    void aplicaMigracionesYConfiguraTablasDeReportes() throws Exception {
        Flyway.configure()
                .dataSource(MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword());
             Statement statement = connection.createStatement()) {

            assertEquals(4, cantidadMigracionesAplicadas(statement));
            assertTrue(existeTabla(statement, "eventos_reportes_procesados"));
            assertTrue(existeTabla(statement, "proyecciones_asistencia"));
            assertTrue(existeTabla(statement, "proyecciones_rendimiento"));
            assertTrue(existeTabla(statement, "alertas_riesgo"));
            assertTrue(existeTabla(statement, "notification_event_outbox"));

            statement.executeUpdate("""
                INSERT INTO proyecciones_asistencia
                    (id_estudiante, id_curso_asignatura, id_asistencia, fecha, estado,
                     porcentaje_inasistencia, total_registros, total_inasistencias,
                     nivel_riesgo, id_evento, correlation_id, actualizado_en)
                VALUES (10, 20, 30, '2026-10-01', 'AUSENTE', 60.0, 10, 6,
                        'CRITICO', 'evento-1', 'corr-1', CURRENT_TIMESTAMP(6))
                """);

            assertEquals(1, statement.executeQuery(
                    "SELECT COUNT(*) FROM proyecciones_asistencia").getInt(1));
        }
    }

    private int cantidadMigracionesAplicadas(Statement statement) throws Exception {
        try (ResultSet resultado = statement.executeQuery(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE")) {
            resultado.next();
            return resultado.getInt(1);
        }
    }

    private boolean existeTabla(Statement statement, String tabla) throws Exception {
        try (ResultSet resultado = statement.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = '" + tabla + "'")) {
            resultado.next();
            return resultado.getInt(1) == 1;
        }
    }
}
