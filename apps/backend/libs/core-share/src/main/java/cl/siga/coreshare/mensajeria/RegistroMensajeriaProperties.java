package cl.siga.coreshare.mensajeria;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

/**
 * Nombres de la topología RabbitMQ del registro asíncrono. Los valores viven
 * en la configuración de cada servicio (application.yaml / common-properties)
 * para poder cambiarlos sin recompilar.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "siga.mensajeria.registro")
public class RegistroMensajeriaProperties {

    private String exchange;

    private String azureQueue;
    private String estudiantesQueue;
    private String docentesQueue;
    private String apoderadosQueue;
    private String statusQueue;
    private String credentialsQueue;

    private String azureDlq;
    private String estudiantesDlq;
    private String docentesDlq;
    private String apoderadosDlq;
    private String statusDlq;
    private String credentialsDlq;

    private String rkAzureSync;
    private String rkRegisterEstudiante;
    private String rkRegisterDocente;
    private String rkRegisterApoderado;
    private String rkStatus;
    private String rkCredentialsNotify;

    private String headerCorrelationId;

    @PostConstruct
    public void validar() {
        requerido(exchange, "exchange");
        requerido(azureQueue, "azure-queue");
        requerido(estudiantesQueue, "estudiantes-queue");
        requerido(docentesQueue, "docentes-queue");
        requerido(apoderadosQueue, "apoderados-queue");
        requerido(statusQueue, "status-queue");
        requerido(credentialsQueue, "credentials-queue");
        requerido(azureDlq, "azure-dlq");
        requerido(estudiantesDlq, "estudiantes-dlq");
        requerido(docentesDlq, "docentes-dlq");
        requerido(apoderadosDlq, "apoderados-dlq");
        requerido(statusDlq, "status-dlq");
        requerido(credentialsDlq, "credentials-dlq");
        requerido(rkAzureSync, "rk-azure-sync");
        requerido(rkRegisterEstudiante, "rk-register-estudiante");
        requerido(rkRegisterDocente, "rk-register-docente");
        requerido(rkRegisterApoderado, "rk-register-apoderado");
        requerido(rkStatus, "rk-status");
        requerido(rkCredentialsNotify, "rk-credentials-notify");
        requerido(headerCorrelationId, "header-correlation-id");
    }

    private static void requerido(String valor, String clave) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                "Falta configurar siga.mensajeria.registro." + clave);
        }
    }
}
