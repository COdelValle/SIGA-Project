package cl.siga.msusuariosauth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import cl.siga.msusuariosauth.config.RegistroAsyncProperties;

class CredentialCipherErroresTest {

    private CredentialCipher cipher() {
        RegistroAsyncProperties properties = new RegistroAsyncProperties();
        properties.setEnabled(true);
        properties.setCredentialKey(Base64.getEncoder().encodeToString(new byte[32]));
        return new CredentialCipher(properties);
    }

    @Test
    void descifrarConBase64InvalidoLanzaIllegalState() {
        assertThatThrownBy(() -> cipher().descifrar("no-es-base64", " tampoco "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("descifrar");
    }

    @Test
    void descifrarConCiphertextAlteradoLanzaIllegalState() {
        CredentialCipher cipher = cipher();
        CredentialCipher.Cifrado cifrado = cipher.cifrar("texto de prueba");
        String alterado = (cifrado.ciphertext().charAt(0) == 'A' ? "B" : "A")
                + cifrado.ciphertext().substring(1);

        assertThatThrownBy(() -> cipher.descifrar(alterado, cifrado.iv()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("descifrar");
    }
}
