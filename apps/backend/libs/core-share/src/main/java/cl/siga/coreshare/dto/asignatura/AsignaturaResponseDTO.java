package cl.siga.coreshare.dto.asignatura;

import cl.siga.coreshare.enums.AreaAcademica;

/**
 * Asignatura del catálogo general. No incluye curso, docente, semestre ni
 * horarios: esos datos pertenecen a la dictación ({@code CursoAsignaturaResponseDTO}).
 */
public record AsignaturaResponseDTO(
    Long id,
    String nombre,
    String nombreCorto,
    String descripcion,
    AreaAcademica area,
    boolean calificable,
    boolean activa
) {}
