package cl.siga.bffweb.domain.estudiantes.dto.internal;

import java.util.List;

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.enums.AreaAcademica;

public record AsignaturaDetalleDTO(
    Long id,
    Long idAsignatura,
    String name,
    String description,
    AreaAcademica area,
    CaracterAsignatura caracter,
    boolean calificable,
    Long idDocente,
    String docente,
    List<HorarioDetalleDTO> horarios,
    List<EvaluacionDetalleDTO> evaluaciones
) {
}
