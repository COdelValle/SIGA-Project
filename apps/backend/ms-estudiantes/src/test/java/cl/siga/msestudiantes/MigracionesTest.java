package cl.siga.msestudiantes;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Aplica las migraciones Flyway sobre un MariaDB real y valida las restricciones
 * criticas (unicidad de idUsuario).
 */
@Testcontainers(disabledWithoutDocker = true)
class MigracionesTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
            .withDatabaseName("siga_estudiantes_db")
            .withUsername("siga")
            .withPassword("siga");

    @Test
    void aplicaMigracionesYValidaIdUsuarioUnico() throws Exception {
        Flyway.configure()
                .dataSource(MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword());
             Statement statement = connection.createStatement()) {

            var seed = statement.executeQuery("SELECT COUNT(*) FROM estudiantes");
            assertTrue(seed.next());
            assertTrue(seed.getInt(1) >= 2, "deberia haber estudiantes de seed");

            statement.executeUpdate(
                    "INSERT INTO estudiantes (id_usuario, rut, first_name, first_surname, birth_date, state) "
                            + "VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '99999999-9', 'TEST', 'TEST', '2010-01-01', 'REGISTRADO')");

            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO estudiantes (id_usuario, rut, first_name, first_surname, birth_date, state) "
                            + "VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '88888888-8', 'OTRO', 'OTRO', '2011-01-01', 'REGISTRADO')"));
        }
    }
}
