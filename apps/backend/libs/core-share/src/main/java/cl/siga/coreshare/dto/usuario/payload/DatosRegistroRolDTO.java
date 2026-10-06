package cl.siga.coreshare.dto.usuario.payload;

import jakarta.validation.Valid;

/**
 * Payload tipado del registro compuesto: exactamente un bloque debe venir
 * informado y debe corresponder al rol solicitado. La consistencia rol-payload
 * se valida en el servicio de registro (no se expresa con bean validation para
 * mantener el mensaje de error claro).
 */
public record DatosRegistroRolDTO(
    @Valid
    DatosRegistroEstudianteDTO estudiante,

    @Valid
    DatosRegistroDocenteDTO docente,

    @Valid
    DatosRegistroApoderadoDTO apoderado
) {
}
