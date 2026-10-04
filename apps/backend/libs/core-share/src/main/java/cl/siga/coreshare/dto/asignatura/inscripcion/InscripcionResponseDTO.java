package cl.siga.coreshare.dto.asignatura.inscripcion;

import java.time.LocalDateTime;

import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;

public record InscripcionResponseDTO(
    Long id,
    Long idAlumno,
    Long idCursoAsignatura,
    EstadoInscripcion estado,
    LocalDateTime fechaInscripcion
) {}