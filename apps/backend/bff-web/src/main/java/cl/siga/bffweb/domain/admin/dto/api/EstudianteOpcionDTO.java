package cl.siga.bffweb.domain.admin.dto.api;

/** Opción de estudiante para el buscador del registro de apoderados. */
public record EstudianteOpcionDTO(
    Long id,
    String rut,
    String firstName,
    String firstSurname
) {
}
