package cl.siga.coreshare.mensajeria;

/**
 * Nombres de la mensajería de notificaciones.
 * Un solo intercambio Topic reparte a las 3 colas futuras
 * según la clave de enrutamiento (routing key).
 */
public final class NombresMensajeria {

    private NombresMensajeria() {
    }

    /** Intercambio Topic: recibe todo lo de notificaciones y lo reparte. */
    public static final String INTERCAMBIO_NOTIFICACIONES = "intercambio-notificaciones";

    /** Cola 1 (la que hacemos ahora): eventos de evaluaciones. */
    public static final String COLA_EVALUACIONES = "cola-notificaciones-evaluaciones";

    /** Claves de enrutamiento de evaluaciones. */
    public static final String CLAVE_EVALUACION_CREADA = "evaluacion.creada";
    public static final String CLAVE_EVALUACION_ACTUALIZADA = "evaluacion.actualizada";
    public static final String CLAVE_EVALUACION_ELIMINADA = "evaluacion.eliminada";

    /** Patrón que atrapa las 3 claves de evaluación en la misma cola. */
    public static final String PATRON_EVALUACION = "evaluacion.*";

    /** Cola 2: eventos de asistencias (solo al crear: AUSENTE o ATRASADO). */
    public static final String COLA_ASISTENCIAS = "cola-notificaciones-asistencias";

    /** Clave de enrutamiento de asistencias. */
    public static final String CLAVE_ASISTENCIA_REGISTRADA = "asistencia.registrada";

    /** Patrón que atrapa las claves de asistencia en la misma cola. */
    public static final String PATRON_ASISTENCIA = "asistencia.*";

    /** Cola 3: eventos de notas (al crear y modificar). */
    public static final String COLA_NOTAS = "cola-notificaciones-notas";

    /** Claves de enrutamiento de notas. */
    public static final String CLAVE_NOTA_CREADA = "nota.creada";
    public static final String CLAVE_NOTA_ACTUALIZADA = "nota.actualizada";

    /** Patrón que atrapa las claves de nota en la misma cola. */
    public static final String PATRON_NOTA = "nota.*";

    /** Umbral de alerta: 60% de inasistencia en la dictación durante el mes. */
    public static final double UMBRAL_INASISTENCIA = 60.0;
}
