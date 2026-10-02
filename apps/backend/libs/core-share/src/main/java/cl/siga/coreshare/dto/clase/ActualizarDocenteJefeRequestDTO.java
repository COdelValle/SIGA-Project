package cl.siga.coreshare.dto.clase;

/**
 * Permite asignar o limpiar (null) el docente jefe de una clase.
 */
public record ActualizarDocenteJefeRequestDTO(
    Long idDocenteJefe
) {}
