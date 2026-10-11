package cl.siga.msusuariosauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuración del registro asíncrono de usuarios. Deshabilitado por defecto:
 * activarlo requiere Graph configurado y la clave de cifrado de credenciales.
 */
@Component
@ConfigurationProperties(prefix = "siga.registro-async")
@Getter
@Setter
public class RegistroAsyncProperties {

    private boolean enabled;
    private boolean notifyCredentialsEnabled;

    /** Clave AES-256 en Base64 (32 bytes) para cifrar la clave temporal en reposo. */
    private String credentialKey;

    private int credentialTtlHours = 48;
    private long outboxPollMs = 2000;
    private int outboxMaxAttempts = 10;
    private long outboxConfirmTimeoutSeconds = 5;

    /** Dominio institucional para el correo generado a partir del nombre. */
    private String emailDomain = "platformsiga.onmicrosoft.com";

    public boolean isNotConfigured() {
        return credentialKey == null || credentialKey.isBlank();
    }
}
