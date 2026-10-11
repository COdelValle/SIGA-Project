package cl.siga.bffweb.domain.notas.dto;

public record NotaCursoDTO(
    Long id,
    Long idEvaluacion,
    Double score
) {
}
