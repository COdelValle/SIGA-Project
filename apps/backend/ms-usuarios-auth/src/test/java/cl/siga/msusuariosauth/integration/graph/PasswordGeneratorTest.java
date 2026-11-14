package cl.siga.msusuariosauth.integration.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordGeneratorTest {

    @Test
    void generaClavesDe16CaracteresConTodasLasCategorias() {
        for (int i = 0; i < 50; i++) {
            String password = PasswordGenerator.generar();

            assertEquals(16, password.length());
            assertTrue(password.chars().anyMatch(Character::isUpperCase));
            assertTrue(password.chars().anyMatch(Character::isLowerCase));
            assertTrue(password.chars().anyMatch(Character::isDigit));
            assertTrue(password.chars().anyMatch(c -> "!@#$%*?-_".indexOf(c) >= 0));
            assertTrue(password.chars().noneMatch(c -> "0O1lI".indexOf(c) >= 0));
        }
    }

    @Test
    void generaClavesDistintas() {
        assertNotEquals(PasswordGenerator.generar(), PasswordGenerator.generar());
    }
}
