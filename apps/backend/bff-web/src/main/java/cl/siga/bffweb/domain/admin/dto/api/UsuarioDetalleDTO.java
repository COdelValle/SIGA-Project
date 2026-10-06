package cl.siga.bffweb.domain.admin.dto.api;

import java.util.List;

/**
 * Detalle de un usuario para la vista del administrador: datos de la cuenta más
 * un resumen del perfil del rol (RUT, fecha, clase/área/teléfonos y etiquetas).
 */
public record UsuarioDetalleDTO(
    String id,
    String fullName,
    String email,
    String rol,
    String estado,
    String rut,
    String fechaNacimiento,
    String detalle,
    List<String> etiquetas
) {
}
