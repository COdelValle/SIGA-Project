package cl.siga.bffweb.domain.docentes.dto;

public record AlumnoDTO(
    Long id,
    String nombres,
    String firstSurname,
    String secondSurname
) {
}
