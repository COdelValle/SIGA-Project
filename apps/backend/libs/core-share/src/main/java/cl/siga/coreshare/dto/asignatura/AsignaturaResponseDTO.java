package cl.siga.coreshare.dto.asignatura;

import java.util.List;

import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.enums.TipoAsignatura;
import cl.siga.coreshare.dto.asignatura.horario.HorarioResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.coreshare.enums.AreaAcademica;

public record AsignaturaResponseDTO(
    Long id,
    String name,
    String description,
    Semestre semestre,
    AreaAcademica area,
    TipoAsignatura tipo,
    Long idDocente,
    
    // Lista de horarios con sus IDs individuales
    List<HorarioResponseDTO> horarios,

    // Exclusivo para Asignatura Básica (null si es Electiva)
    Long idClase,

    // Exclusivos para Asignatura Electiva (nulls si es Básica)
    Integer cupoMaximo,
    Integer cuposDisponibles,
    Integer totalInscritos,
    List<InscripcionResponseDTO> inscripciones
) {}