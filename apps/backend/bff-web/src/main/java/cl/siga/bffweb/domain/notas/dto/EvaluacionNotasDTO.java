package cl.siga.bffweb.domain.notas.dto;

public record EvaluacionNotasDTO(
    Long id,
    String nombre,
    String tipo,
    Double ponderacion
) {
}
