package cl.siga.coreshare.dto.asignatura.curso;

import java.util.List;

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.horario.HorarioRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record RegistrarCursoAsignaturaRequestDTO(
    @NotNull(message = "El ID de la asignatura del catálogo es obligatorio")
    Long idAsignatura,

    @NotNull(message = "El ID de la clase es obligatorio")
    Long idClase,

    @NotNull(message = "El ID del docente es obligatorio")
    Long idDocente,

    @NotNull(message = "El semestre es obligatorio")
    Semestre semestre,

    @NotNull(message = "El carácter de la asignatura es obligatorio")
    CaracterAsignatura caracter,

    @Min(value = 1, message = "El cupo máximo debe ser al menos 1")
    Integer cupoMaximo,

    @NotEmpty(message = "Debe asignar al menos un horario al registrar la dictación")
    @Valid
    List<HorarioRequestDTO> horarios
) {}
