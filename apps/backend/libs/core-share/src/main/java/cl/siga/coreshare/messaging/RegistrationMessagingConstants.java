package cl.siga.coreshare.messaging;

public final class RegistrationMessagingConstants {
    private RegistrationMessagingConstants() {
    }

    public static final String EXCHANGE = "user.topic.exchange";

    public static final String AZURE_QUEUE = "user.azure.sync.queue";
    public static final String ESTUDIANTES_QUEUE = "ms.estudiantes.queue";
    public static final String DOCENTES_QUEUE = "ms.docentes.queue";
    public static final String APODERADOS_QUEUE = "ms.apoderados.queue";
    public static final String STATUS_QUEUE = "user.registration.status.queue";

    public static final String AZURE_DLQ = "user.azure.sync.dlq";
    public static final String ESTUDIANTES_DLQ = "ms.estudiantes.dlq";
    public static final String DOCENTES_DLQ = "ms.docentes.dlq";
    public static final String APODERADOS_DLQ = "ms.apoderados.dlq";
    public static final String STATUS_DLQ = "user.registration.status.dlq";

    public static final String RK_REGISTER_ESTUDIANTE = "user.register.estudiante";
    public static final String RK_REGISTER_DOCENTE = "user.register.docente";
    public static final String RK_REGISTER_APODERADO = "user.register.apoderado";
    public static final String RK_REGISTER_ADMIN = "user.register.admin";
    public static final String RK_STATUS = "user.registration.status";
}
