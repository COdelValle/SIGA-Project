package cl.siga.msasignaturas.model;

import java.util.List;

import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;

/**
 * Estados de inscripcion que ocupan cupo en una asignatura electiva.
 * Fuente unica para cupos disponibles y validacion de reduccion de cupo.
 */
public final class InscripcionEstados {

    public static final List<EstadoInscripcion> OCUPAN_CUPO = List.of(
            EstadoInscripcion.ACTIVO,
            EstadoInscripcion.PRE_INSCRITO
    );

    private InscripcionEstados() {
    }
}
