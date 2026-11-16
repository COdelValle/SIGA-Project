package cl.siga.coreshare.messaging;

import cl.siga.coreshare.mensajeria.ConfiguracionNombresRabbitMQ;

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

    public static final String EXCHANGE = ConfiguracionNombresRabbitMQ.Registro.EXCHANGE;

    public static final String AZURE_QUEUE = ConfiguracionNombresRabbitMQ.Registro.AZURE_QUEUE;
    public static final String ESTUDIANTES_QUEUE = ConfiguracionNombresRabbitMQ.Registro.ESTUDIANTES_QUEUE;
    public static final String DOCENTES_QUEUE = ConfiguracionNombresRabbitMQ.Registro.DOCENTES_QUEUE;
    public static final String APODERADOS_QUEUE = ConfiguracionNombresRabbitMQ.Registro.APODERADOS_QUEUE;
    public static final String STATUS_QUEUE = ConfiguracionNombresRabbitMQ.Registro.STATUS_QUEUE;
    public static final String CREDENTIALS_QUEUE = ConfiguracionNombresRabbitMQ.Registro.CREDENTIALS_QUEUE;

    public static final String AZURE_DLQ = ConfiguracionNombresRabbitMQ.Registro.AZURE_DLQ;
    public static final String ESTUDIANTES_DLQ = ConfiguracionNombresRabbitMQ.Registro.ESTUDIANTES_DLQ;
    public static final String DOCENTES_DLQ = ConfiguracionNombresRabbitMQ.Registro.DOCENTES_DLQ;
    public static final String APODERADOS_DLQ = ConfiguracionNombresRabbitMQ.Registro.APODERADOS_DLQ;
    public static final String STATUS_DLQ = ConfiguracionNombresRabbitMQ.Registro.STATUS_DLQ;
    public static final String CREDENTIALS_DLQ = ConfiguracionNombresRabbitMQ.Registro.CREDENTIALS_DLQ;

    /** Etapa de aprovisionamiento en Entra ID (evento inicial). */
    public static final String RK_AZURE_SYNC = ConfiguracionNombresRabbitMQ.Registro.RK_AZURE_SYNC;

    /** Evento de dominio, ya con el oid resuelto, hacia el microservicio del rol. */
    public static final String RK_REGISTER_ESTUDIANTE = ConfiguracionNombresRabbitMQ.Registro.RK_REGISTER_ESTUDIANTE;
    public static final String RK_REGISTER_DOCENTE = ConfiguracionNombresRabbitMQ.Registro.RK_REGISTER_DOCENTE;
    public static final String RK_REGISTER_APODERADO = ConfiguracionNombresRabbitMQ.Registro.RK_REGISTER_APODERADO;

    /** Resultado de la etapa de dominio publicada por los microservicios de rol. */
    public static final String RK_STATUS = ConfiguracionNombresRabbitMQ.Registro.RK_STATUS;

    /** Reservada para el futuro servicio de notificaciones (entrega por correo). */
    public static final String RK_CREDENTIALS_NOTIFY = ConfiguracionNombresRabbitMQ.Registro.RK_CREDENTIALS_NOTIFY;

    public static final String HEADER_CORRELATION_ID = ConfiguracionNombresRabbitMQ.Registro.HEADER_CORRELATION_ID;
}
