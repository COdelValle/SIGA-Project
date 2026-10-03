package cl.siga.bffweb.domain.estudiantes.dto.internal;

public record HorarioDetalleDTO(
    Long id,
    String dia,
    String horarioEntrada,
    String horarioSalida,
    String ubicacion
) {
}
