package cl.siga.bffweb.domain.estudiantes.dto.internal;

import java.util.List;

public record AsignaturaDetalleDTO(
    Long id,
    String name,
    String description,
    List<NotaDetalleDTO> notas
) {

}
