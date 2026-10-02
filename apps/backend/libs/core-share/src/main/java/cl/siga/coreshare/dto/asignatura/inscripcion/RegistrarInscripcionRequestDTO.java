package cl.siga.coreshare.dto.asignatura.inscripcion;

import jakarta.validation.constraints.NotNull;

public record RegistrarInscripcionRequestDTO(
        @NotNull(message = "El ID del alumno es obligatorio") 
        Long idAlumno,
        
        @NotNull(message = "El ID de la asignatura es obligatorio") 
        Long idAsignatura
) {}