package cl.siga.bffweb.domain.admin.dto;

import java.util.List;

import cl.siga.coreshare.enums.AreaAcademica;

public record AsignaturaAdminDTO(
    Long id,
    String nombre,
    AreaAcademica area,
    boolean calificable,
    List<String> niveles,
    boolean activa
) {
}
