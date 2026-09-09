package cl.siga.coreshare.dto.notas;

public record NotaResponseDTO(
    Long id,
    Long idEstudiante,
    Long idAsignatura,
    Double score
) {
}
