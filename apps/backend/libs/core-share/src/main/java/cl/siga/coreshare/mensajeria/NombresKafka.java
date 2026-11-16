package cl.siga.coreshare.mensajeria;

/** Nombres y grupos compartidos de los flujos Kafka académicos. */
public final class NombresKafka {

    private NombresKafka() {
    }

    public static final String TOPIC_ASISTENCIAS = "siga.academico.asistencias";
    public static final String TOPIC_RENDIMIENTO = "siga.academico.rendimiento";

    public static final String GRUPO_REPORTES_ASISTENCIAS = "siga-reportes-asistencias";
    public static final String GRUPO_REPORTES_RENDIMIENTO = "siga-reportes-rendimiento";
}
