package cl.siga.coreshare.dto.notas;

public record NotaResponseDTO(
    Long id,
    Long idEstudiante,
    Long idEvaluacion,
    Double score
) {
}
