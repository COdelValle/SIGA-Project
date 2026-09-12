package cl.siga.msusuariosauth.integration.graph;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Credenciales de aplicación (client credentials) para llamar a Microsoft Graph.
 * Si {@code client-secret} no está definido, la integración queda deshabilitada
 * y los endpoints que dependen de Graph responden con un error de negocio.
 */
@Component
@ConfigurationProperties(prefix = "azure.graph")
@Getter
@Setter
public class GraphProperties {

    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String apiAppId;

    public boolean isConfigured() {
        return isNotBlank(tenantId) && isNotBlank(clientId) && isNotBlank(clientSecret);
    }

    public String apiAppIdOrDefault() {
        return isNotBlank(apiAppId) ? apiAppId : clientId;
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
