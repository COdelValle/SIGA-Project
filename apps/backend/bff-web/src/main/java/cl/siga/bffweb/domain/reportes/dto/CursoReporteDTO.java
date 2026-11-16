package cl.siga.bffweb.domain.reportes.dto;

/** Opción de curso/asignatura para los filtros del dashboard de reportes. */
public record CursoReporteDTO(
    Long id,
    String nombre,
    String curso,
    Long idClase
) {
}
