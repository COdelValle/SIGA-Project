package cl.siga.bffweb.domain.admin.dto.api;

/** Opción de clase para el selector del registro de estudiantes. */
public record ClaseOpcionDTO(
    long id,
    String nivel,
    String letra,
    Integer anioAcademico
) {
}
