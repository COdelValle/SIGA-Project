package cl.siga.msusuariosauth.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import cl.siga.msusuariosauth.config.RegistroAsyncProperties;

/**
 * Cifra y descifra la clave temporal del registro asíncrono con AES-256-GCM.
 * La clave se inyecta por entorno ({@code REGISTRO_CRED_KEY}, Base64 de 32
 * bytes) y es obligatoria cuando el flujo está habilitado.
 */
@Component
public class CredentialCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RegistroAsyncProperties properties;
    private volatile SecretKeySpec key;

    public CredentialCipher(RegistroAsyncProperties properties) {
        this.properties = properties;
        if (properties.isEnabled() && properties.isNotConfigured()) {
            throw new IllegalStateException(
                    "El registro asíncrono está habilitado pero falta REGISTRO_CRED_KEY "
                            + "(clave AES-256 en Base64 de 32 bytes).");
        }
    }

    public Cifrado cifrar(String valor) {
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, clave(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            return new Cifrado(
                    Base64.getEncoder().encodeToString(cifrado),
                    Base64.getEncoder().encodeToString(iv));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo cifrar la credencial temporal.", ex);
        }
    }

    public String descifrar(String ciphertextBase64, String ivBase64) {
        try {
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] cifrado = Base64.getDecoder().decode(ciphertextBase64);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("No se pudo descifrar la credencial temporal.", ex);
        }
    }

    private SecretKeySpec clave() {
        SecretKeySpec actual = key;
        if (actual == null) {
            byte[] bytes = Base64.getDecoder().decode(properties.getCredentialKey());
            if (bytes.length != KEY_BYTES) {
                throw new IllegalStateException("REGISTRO_CRED_KEY debe decodificar a 32 bytes (AES-256).");
            }
            actual = new SecretKeySpec(bytes, "AES");
            key = actual;
        }
        return actual;
    }

    public record Cifrado(String ciphertext, String iv) {
    }
}
