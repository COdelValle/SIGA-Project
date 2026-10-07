package cl.siga.coreshare.messaging;

/**
 * Topología de mensajería del registro asíncrono. Los nombres viven en
 * core-share para que productor y consumidores no puedan divergir.
 *
 * <p>Flujo: {@code startRegistration} publica el evento inicial en
 * {@code RK_AZURE_SYNC}. Al completarse el aprovisionamiento en Entra ID,
 * ms-usuarios-auth publica el mismo evento (con {@code userId}) en
 * {@code RK_REGISTER_*} y el microservicio del rol responde a
 * {@code RK_STATUS}. La entrega de credenciales por correo (futuro) usa
 * {@code RK_CREDENTIALS_NOTIFY}.</p>
 */
public final class RegistrationMessagingConstants {
    private RegistrationMessagingConstants() {
    }

    public static final String EXCHANGE = "user.topic.exchange";

    public static final String AZURE_QUEUE = "user.azure.sync.queue";
    public static final String ESTUDIANTES_QUEUE = "ms.estudiantes.queue";
    public static final String DOCENTES_QUEUE = "ms.docentes.queue";
    public static final String APODERADOS_QUEUE = "ms.apoderados.queue";
    public static final String STATUS_QUEUE = "user.registration.status.queue";
    public static final String CREDENTIALS_QUEUE = "user.credentials.notify.queue";

    public static final String AZURE_DLQ = "user.azure.sync.dlq";
    public static final String ESTUDIANTES_DLQ = "ms.estudiantes.dlq";
    public static final String DOCENTES_DLQ = "ms.docentes.dlq";
    public static final String APODERADOS_DLQ = "ms.apoderados.dlq";
    public static final String STATUS_DLQ = "user.registration.status.dlq";
    public static final String CREDENTIALS_DLQ = "user.credentials.notify.dlq";

    /** Etapa de aprovisionamiento en Entra ID (evento inicial). */
    public static final String RK_AZURE_SYNC = "user.registration.azure";

    /** Evento de dominio, ya con el oid resuelto, hacia el microservicio del rol. */
    public static final String RK_REGISTER_ESTUDIANTE = "user.register.estudiante";
    public static final String RK_REGISTER_DOCENTE = "user.register.docente";
    public static final String RK_REGISTER_APODERADO = "user.register.apoderado";

    /** Resultado de la etapa de dominio publicada por los microservicios de rol. */
    public static final String RK_STATUS = "user.registration.status";

    /** Reservada para el futuro servicio de notificaciones (entrega por correo). */
    public static final String RK_CREDENTIALS_NOTIFY = "user.credentials.notify";

    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
}
