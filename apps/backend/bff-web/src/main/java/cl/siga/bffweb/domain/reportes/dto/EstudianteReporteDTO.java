package cl.siga.bffweb.domain.reportes.dto;

/** Opción de estudiante para los filtros del dashboard de reportes. */
public record EstudianteReporteDTO(
    Long id,
    String nombre,
    String rut
) {
}
