package cl.siga.coreshare.dto.asignatura;

import java.util.List;

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.horario.HorarioResponseDTO;
import cl.siga.coreshare.enums.AreaAcademica;

/**
 * Dictación concreta de una asignatura del catálogo en un curso, con su
 * docente, semestre, horarios y cupos. Es lo que antes se modelaba como
 * "asignatura básica/electiva" duplicando el catálogo.
 */
public record CursoAsignaturaResponseDTO(
    Long id,
    Long idAsignatura,
    String nombre,
    String descripcion,
    AreaAcademica area,
    boolean calificable,
    CaracterAsignatura caracter,
    Semestre semestre,
    Long idDocente,
    Long idClase,
    Integer cupoMaximo,
    Integer cuposDisponibles,
    Integer totalInscritos,
    List<HorarioResponseDTO> horarios
) {}
