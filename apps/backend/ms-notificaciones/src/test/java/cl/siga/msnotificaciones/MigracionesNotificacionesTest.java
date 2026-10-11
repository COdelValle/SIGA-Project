package cl.siga.msnotificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

@Testcontainers(disabledWithoutDocker = true)
class MigracionesNotificacionesTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
        .withDatabaseName("siga_notificaciones_db")
        .withUsername("siga")
        .withPassword(UUID.randomUUID().toString());

    @Test
    void creaBandejaLecturasYGarantizaIdempotenciaDelEvento() throws Exception {
        Flyway.configure()
            .dataSource(MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword())
            .locations("classpath:db/migration")
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(
                MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO notificaciones
                    (id_evento, tipo, accion, tipo_destino, id_destino, titulo, resumen, fecha_hora)
                VALUES ('evento-unico', 'NOTA', 'CREADA', 'ESTUDIANTE', 41,
                    'Nueva calificación', 'Hay una nueva calificación en SIGA.', CURRENT_TIMESTAMP(6))
                """);

            assertEquals(0, statement.executeUpdate("""
                INSERT IGNORE INTO notificaciones
                    (id_evento, tipo, accion, tipo_destino, id_destino, titulo, resumen, fecha_hora)
                VALUES ('evento-unico', 'NOTA', 'CREADA', 'ESTUDIANTE', 41,
                    'Nueva calificación', 'Hay una nueva calificación en SIGA.', CURRENT_TIMESTAMP(6))
                """));

            assertEquals(1, statement.executeUpdate("""
                INSERT INTO notificacion_lecturas (id_notificacion, id_usuario, leida_en)
                SELECT id, 'oid-estudiante', CURRENT_TIMESTAMP(6)
                FROM notificaciones WHERE id_evento = 'evento-unico'
                """));
            assertEquals(0, statement.executeUpdate("""
                INSERT IGNORE INTO notificacion_lecturas (id_notificacion, id_usuario, leida_en)
                SELECT id, 'oid-estudiante', CURRENT_TIMESTAMP(6)
                FROM notificaciones WHERE id_evento = 'evento-unico'
                """));

            // V2: ocultamiento por usuario ("Limpiar mis leídas") sin borrar la fila.
            assertEquals(1, statement.executeUpdate("""
                UPDATE notificacion_lecturas
                SET ocultada_en = CURRENT_TIMESTAMP(6)
                WHERE id_usuario = 'oid-estudiante'
                """));

            var conteo = statement.executeQuery("SELECT COUNT(*) FROM notificaciones");
            assertTrue(conteo.next());
            assertEquals(1, conteo.getInt(1));

            var ocultas = statement.executeQuery("""
                SELECT COUNT(*) FROM notificacion_lecturas
                WHERE ocultada_en IS NOT NULL
                """);
            assertTrue(ocultas.next());
            assertEquals(1, ocultas.getInt(1));
        }
    }
}
