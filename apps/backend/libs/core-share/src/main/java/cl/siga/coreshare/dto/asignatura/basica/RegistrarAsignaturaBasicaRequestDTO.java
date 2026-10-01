package cl.siga.coreshare.dto.asignatura.basica;

import java.util.List;

import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.horario.HorarioRequestDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record RegistrarAsignaturaBasicaRequestDTO(
    @NotBlank (message = "El nombre es requerido")
    @Size (min = 2, max = 50)
    String name,

    @NotBlank (message = "La descripción es requerida")
    @Size (min = 2, max = 150)
    String description,
    
    @NotNull(message = "El semestre es obligatorio")
    Semestre semestre,

    @NotNull(message = "El área académica es obligatoria")
    AreaAcademica area,

    @NotNull(message = "El ID del docente es obligatorio")
    Long idDocente,

    @NotEmpty(message = "Debe asignar al menos un horario al registrar la asignatura")
    @Valid 
    List<HorarioRequestDTO> horarios,

    @NotNull(message = "El ID de la clase es obligatorio para asignaturas básicas")
    Long idClase
) {}