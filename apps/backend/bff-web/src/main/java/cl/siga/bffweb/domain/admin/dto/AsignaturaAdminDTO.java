package cl.siga.bffweb.domain.admin.dto;

public record AsignaturaAdminDTO(
    Long id,
    String nombre,
    String descripcion,
    boolean activa
) {
}
