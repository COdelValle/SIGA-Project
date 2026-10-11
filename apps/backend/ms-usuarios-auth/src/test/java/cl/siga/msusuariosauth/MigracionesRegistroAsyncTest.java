package cl.siga.msusuariosauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Valida sobre MariaDB real las migraciones V5-V7 del registro asíncrono:
 * columnas de credencial/version en el proceso, outbox y unicidad del evento.
 */
@Testcontainers(disabledWithoutDocker = true)
class MigracionesRegistroAsyncTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
            .withDatabaseName("siga_usuarios_db")
            .withUsername("siga")
            .withPassword(UUID.randomUUID().toString());

    @Test
    void aplicaMigracionesYValidaEsquemaDeRegistroAsync() throws Exception {
        Flyway.configure()
                .dataSource(MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword());
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(
                    "INSERT INTO user_registration_processes "
                            + "(process_id, event_id, correlation_id, email, full_name, requested_role, "
                            + "state, azure_state, domain_state, role_data) "
                            + "VALUES ('p-1', 'e-1', 'c-1', 'nuevo@test.com', 'Nuevo Usuario', 'DOCENTE', "
                            + "'EN_PROCESO', 'PENDIENTE', 'PENDIENTE', '{}')");

            var proceso = statement.executeQuery(
                    "SELECT version, credential_ciphertext FROM user_registration_processes WHERE process_id = 'p-1'");
            assertTrue(proceso.next());
            assertEquals(0L, proceso.getLong("version"));
            assertEquals(null, proceso.getString("credential_ciphertext"));

            statement.executeUpdate(
                    "INSERT INTO user_registration_outbox "
                            + "(event_id, process_id, exchange, routing_key, payload, state, attempts) "
                            + "VALUES ('oe-1', 'p-1', 'user.topic.exchange', 'user.registration.azure', '{}', "
                            + "'PENDIENTE', 0)");

            var outbox = statement.executeQuery(
                    "SELECT COUNT(*) FROM user_registration_outbox WHERE process_id = 'p-1'");
            assertTrue(outbox.next());
            assertEquals(1, outbox.getInt(1));

            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO user_registration_outbox "
                            + "(event_id, process_id, exchange, routing_key, payload, state, attempts) "
                            + "VALUES ('oe-1', 'p-1', 'user.topic.exchange', 'user.registration.azure', '{}', "
                            + "'PENDIENTE', 0)"));
        }
    }
}
