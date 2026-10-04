package cl.siga.coreshare.dto.usuario;

import java.util.List;

/**
 * Resumen del procesamiento de una carga masiva de invitaciones. Las filas
 * inválidas no bloquean al resto del lote; se informan con su motivo.
 */
public record InvitacionLoteResponseDTO(
    int creadas,
    int duplicadas,
    List<Invalida> invalidas
) {
    public record Invalida(
        String email,
        String motivo
    ) {
    }
}
