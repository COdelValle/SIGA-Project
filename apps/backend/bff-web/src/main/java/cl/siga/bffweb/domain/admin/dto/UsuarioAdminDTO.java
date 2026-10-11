package cl.siga.bffweb.domain.admin.dto;

public record UsuarioAdminDTO(
    String id,
    String nombre,
    String email,
    String rol,
    String estado
) {
}
