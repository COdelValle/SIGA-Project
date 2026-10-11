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
 * Aplica las migraciones Flyway sobre un MariaDB real y valida la tabla de
 * invitaciones (unicidad del correo).
 */
@Testcontainers(disabledWithoutDocker = true)
class MigracionesTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
            .withDatabaseName("siga_usuarios_db")
            .withUsername("siga")
            .withPassword(UUID.randomUUID().toString());

    @Test
    void aplicaMigracionesYValidaInvitaciones() throws Exception {
        Flyway.configure()
                .dataSource(MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword());
             Statement statement = connection.createStatement()) {

            var usuarios = statement.executeQuery("SELECT COUNT(*) FROM usuarios");
            assertTrue(usuarios.next());
            assertTrue(usuarios.getInt(1) >= 4, "deberia haber usuarios de seed");

            statement.executeUpdate(
                    "INSERT INTO invitaciones_usuarios (email, rol, state) "
                            + "VALUES ('invitado@test.com', 'DOCENTE', 'INVITADO')");

            var invitaciones = statement.executeQuery(
                    "SELECT COUNT(*) FROM invitaciones_usuarios WHERE email = 'invitado@test.com'");
            assertTrue(invitaciones.next());
            assertEquals(1, invitaciones.getInt(1));

            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO invitaciones_usuarios (email, rol, state) "
                            + "VALUES ('invitado@test.com', 'ESTUDIANTE', 'INVITADO')"));
        }
    }
}
