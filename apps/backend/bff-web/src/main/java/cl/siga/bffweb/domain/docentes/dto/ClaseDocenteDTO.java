package cl.siga.bffweb.domain.docentes.dto;

public record ClaseDocenteDTO(
    Integer franja,
    Long cursoId,
    String curso,
    String asignatura,
    String sala,
    String dia,
    String horaInicio,
    String horaFin
) {
}
