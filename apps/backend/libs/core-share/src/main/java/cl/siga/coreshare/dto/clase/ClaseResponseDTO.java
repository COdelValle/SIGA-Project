package cl.siga.coreshare.dto.clase;

import cl.siga.coreshare.dto.clase.enums.Nivel;

public record ClaseResponseDTO(
    long id,
    Nivel nivel,
    String letra,
    Integer anioAcademico,
    Long idDocenteJefe
) {

}
