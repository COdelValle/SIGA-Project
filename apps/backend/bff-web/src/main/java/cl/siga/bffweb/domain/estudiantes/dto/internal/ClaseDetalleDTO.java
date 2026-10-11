package cl.siga.bffweb.domain.estudiantes.dto.internal;

public record ClaseDetalleDTO(
    Long id,
    String nivel,
    String letra,
    Integer anioAcademico
) {
}
