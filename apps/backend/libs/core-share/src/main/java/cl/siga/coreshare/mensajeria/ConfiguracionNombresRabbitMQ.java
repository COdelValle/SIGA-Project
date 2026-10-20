package cl.siga.coreshare.mensajeria;

import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * Fuente central de nombres/routing keys de las dos topologías RabbitMQ.
 * Los aliases históricos viven en NombresMensajeria y
 * RegistrationMessagingConstants para no acoplar los dominios entre sí, pero
 * ningún producer, listener ni declaración mantiene literales duplicados.
 */
@AutoConfiguration
public class ConfiguracionNombresRabbitMQ {

    public static final class Academico {
        private Academico() {}

        public static final String EXCHANGE_NOTIFICACIONES = "intercambio-notificaciones";
        public static final String EXCHANGE_DLQ = "siga.dlx.direct";

        public static final String COLA_EVALUACIONES = "cola-notificaciones-evaluaciones";
        public static final String DLQ_EVALUACIONES = "cola-notificaciones-evaluaciones.dlq";
        public static final String CLAVE_EVALUACION_CREADA = "evaluacion.creada";
        public static final String CLAVE_EVALUACION_ACTUALIZADA = "evaluacion.actualizada";
        public static final String CLAVE_EVALUACION_ELIMINADA = "evaluacion.eliminada";
        public static final String PATRON_EVALUACION = "evaluacion.*";

        public static final String COLA_ASISTENCIAS = "cola-notificaciones-asistencias";
        public static final String DLQ_ASISTENCIAS = "cola-notificaciones-asistencias.dlq";
        public static final String CLAVE_ASISTENCIA_REGISTRADA = "asistencia.registrada";
        public static final String PATRON_ASISTENCIA = "asistencia.*";

        public static final String COLA_NOTAS = "cola-notificaciones-notas";
        public static final String DLQ_NOTAS = "cola-notificaciones-notas.dlq";
        public static final String CLAVE_NOTA_CREADA = "nota.creada";
        public static final String CLAVE_NOTA_ACTUALIZADA = "nota.actualizada";
        public static final String PATRON_NOTA = "nota.*";
    }

    public static final class Registro {
        private Registro() {}

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

        public static final String RK_AZURE_SYNC = "user.registration.azure";
        public static final String RK_REGISTER_ESTUDIANTE = "user.register.estudiante";
        public static final String RK_REGISTER_DOCENTE = "user.register.docente";
        public static final String RK_REGISTER_APODERADO = "user.register.apoderado";
        public static final String RK_STATUS = "user.registration.status";
        public static final String RK_CREDENTIALS_NOTIFY = "user.credentials.notify";
        public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    }
}
