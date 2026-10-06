package cl.siga.msusuariosauth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import cl.siga.msusuariosauth.config.RegistroAsyncProperties;

class CredentialCipherTest {

    private static final String CLAVE = Base64.getEncoder().encodeToString(new byte[32]);

    private RegistroAsyncProperties properties(boolean enabled, String key) {
        RegistroAsyncProperties properties = new RegistroAsyncProperties();
        properties.setEnabled(enabled);
        properties.setCredentialKey(key);
        return properties;
    }

    @Test
    void cifraYDescifraConClaveValida() {
        CredentialCipher cipher = new CredentialCipher(properties(true, CLAVE));

        CredentialCipher.Cifrado cifrado = cipher.cifrar("Secreta-123!");

        assertNotEquals("Secreta-123!", cifrado.ciphertext());
        assertEquals("Secreta-123!", cipher.descifrar(cifrado.ciphertext(), cifrado.iv()));
    }

    @Test
    void dosCifradosUsanIvDistinto() {
        CredentialCipher cipher = new CredentialCipher(properties(true, CLAVE));

        CredentialCipher.Cifrado primero = cipher.cifrar("Secreta-123!");
        CredentialCipher.Cifrado segundo = cipher.cifrar("Secreta-123!");

        assertNotEquals(primero.iv(), segundo.iv());
        assertNotEquals(primero.ciphertext(), segundo.ciphertext());
    }

    @Test
    void fallaAlConstruirHabilitadoSinClave() {
        assertThrows(IllegalStateException.class,
                () -> new CredentialCipher(properties(true, "")));
    }

    @Test
    void fallaConClaveDeLargoInvalido() {
        String claveCorta = Base64.getEncoder().encodeToString(new byte[8]);
        CredentialCipher cipher = new CredentialCipher(properties(true, claveCorta));

        assertThrows(IllegalStateException.class, () -> cipher.cifrar("Secreta-123!"));
    }
}
