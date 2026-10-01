package cl.siga.coreshare.dto.asignatura.inscripcion;

import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;

public record InscripcionResponseDTO(
    Long estudianteId,
    EstadoInscripcion estado
) {}
