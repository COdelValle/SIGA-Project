package cl.siga.bffweb.domain.estudiantes.dto.internal;

public record EvaluacionDetalleDTO(
    Long id,
    String nombre,
    String tipo,
    Double ponderacion,
    Double nota
) {
}
