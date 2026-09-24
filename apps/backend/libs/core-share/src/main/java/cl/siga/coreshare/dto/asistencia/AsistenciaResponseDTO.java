package cl.siga.coreshare.dto.asistencia;

import java.time.LocalDate;

import cl.siga.coreshare.dto.asistencia.enums.Justificacion;
import cl.siga.coreshare.dto.asistencia.enums.State;

public record AsistenciaResponseDTO(
    Long id,
    Long idEstudiante,
    Long idAsignatura,
    LocalDate fecha,
    State estado,
    Justificacion justificacion,
    String observacion
) {

}
