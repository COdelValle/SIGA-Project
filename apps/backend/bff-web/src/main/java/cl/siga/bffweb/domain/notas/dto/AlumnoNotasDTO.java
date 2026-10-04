package cl.siga.bffweb.domain.notas.dto;

import java.util.List;

public record AlumnoNotasDTO(
    Long id,
    String nombres,
    String firstSurname,
    String secondSurname,
    List<NotaCursoDTO> notas
) {
}
