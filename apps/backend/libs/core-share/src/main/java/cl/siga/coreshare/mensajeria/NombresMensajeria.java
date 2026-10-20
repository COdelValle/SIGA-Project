package cl.siga.coreshare.mensajeria;

/**
 * Nombres de la mensajería de notificaciones.
 * Un solo intercambio Topic reparte a las 3 colas académicas
 * según la clave de enrutamiento (routing key).
 */
public final class NombresMensajeria {

    private NombresMensajeria() {
    }

    /** Intercambio Topic: recibe todo lo de notificaciones y lo reparte. */
    public static final String INTERCAMBIO_NOTIFICACIONES =
        ConfiguracionNombresRabbitMQ.Academico.EXCHANGE_NOTIFICACIONES;

    /**
     * Intercambio Direct de dead-lettering: cada DLQ se enlaza con su propio
     * nombre como routing key, de modo que las colas derivan los rechazos aquí.
     */
    public static final String INTERCAMBIO_DLQ = ConfiguracionNombresRabbitMQ.Academico.EXCHANGE_DLQ;

    /** Cola de eventos de evaluaciones. */
    public static final String COLA_EVALUACIONES = ConfiguracionNombresRabbitMQ.Academico.COLA_EVALUACIONES;
    public static final String DLQ_EVALUACIONES = ConfiguracionNombresRabbitMQ.Academico.DLQ_EVALUACIONES;

    /** Claves de enrutamiento de evaluaciones. */
    public static final String CLAVE_EVALUACION_CREADA = ConfiguracionNombresRabbitMQ.Academico.CLAVE_EVALUACION_CREADA;
    public static final String CLAVE_EVALUACION_ACTUALIZADA = ConfiguracionNombresRabbitMQ.Academico.CLAVE_EVALUACION_ACTUALIZADA;
    public static final String CLAVE_EVALUACION_ELIMINADA = ConfiguracionNombresRabbitMQ.Academico.CLAVE_EVALUACION_ELIMINADA;

    /** Patrón que atrapa las 3 claves de evaluación en la misma cola. */
    public static final String PATRON_EVALUACION = ConfiguracionNombresRabbitMQ.Academico.PATRON_EVALUACION;

    /** Cola 2: eventos de asistencias (solo al crear: AUSENTE o ATRASADO). */
    public static final String COLA_ASISTENCIAS = ConfiguracionNombresRabbitMQ.Academico.COLA_ASISTENCIAS;
    public static final String DLQ_ASISTENCIAS = ConfiguracionNombresRabbitMQ.Academico.DLQ_ASISTENCIAS;

    /** Clave de enrutamiento de asistencias. */
    public static final String CLAVE_ASISTENCIA_REGISTRADA = ConfiguracionNombresRabbitMQ.Academico.CLAVE_ASISTENCIA_REGISTRADA;

    /** Patrón que atrapa las claves de asistencia en la misma cola. */
    public static final String PATRON_ASISTENCIA = ConfiguracionNombresRabbitMQ.Academico.PATRON_ASISTENCIA;

    /** Cola 3: eventos de notas (al crear y modificar). */
    public static final String COLA_NOTAS = ConfiguracionNombresRabbitMQ.Academico.COLA_NOTAS;
    public static final String DLQ_NOTAS = ConfiguracionNombresRabbitMQ.Academico.DLQ_NOTAS;

    /** Claves de enrutamiento de notas. */
    public static final String CLAVE_NOTA_CREADA = ConfiguracionNombresRabbitMQ.Academico.CLAVE_NOTA_CREADA;
    public static final String CLAVE_NOTA_ACTUALIZADA = ConfiguracionNombresRabbitMQ.Academico.CLAVE_NOTA_ACTUALIZADA;

    /** Patrón que atrapa las claves de nota en la misma cola. */
    public static final String PATRON_NOTA = ConfiguracionNombresRabbitMQ.Academico.PATRON_NOTA;

    /** Umbral de alerta: 60% de inasistencia en la dictación durante el mes. */
    public static final double UMBRAL_INASISTENCIA = 60.0;
}
