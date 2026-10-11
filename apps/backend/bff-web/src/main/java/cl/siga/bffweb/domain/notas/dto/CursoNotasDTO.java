package cl.siga.bffweb.domain.notas.dto;

import java.util.List;

public record CursoNotasDTO(
    Long asignaturaId,
    String curso,
    String asignatura,
    List<EvaluacionNotasDTO> evaluaciones,
    List<AlumnoNotasDTO> alumnos
) {
}
