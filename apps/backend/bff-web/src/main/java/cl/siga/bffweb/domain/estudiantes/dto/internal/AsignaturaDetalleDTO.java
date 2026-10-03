package cl.siga.bffweb.domain.estudiantes.dto.internal;

import java.util.List;

public record AsignaturaDetalleDTO(
    Long id,
    String name,
    String description,
    Long idDocente,
    String docente,
    List<HorarioDetalleDTO> horarios,
    List<EvaluacionDetalleDTO> evaluaciones
) {
}
