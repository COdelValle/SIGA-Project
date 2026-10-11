package cl.siga.coreshare.dto.asignatura.malla;

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.PlanFormacion;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.enums.AreaAcademica;

/** Fila de la malla curricular: una asignatura aplicada a un nivel. */
public record MallaCurricularResponseDTO(
    Long id,
    Nivel nivel,
    Long idAsignatura,
    String nombre,
    AreaAcademica area,
    CaracterAsignatura caracter,
    PlanFormacion plan,
    Integer horasSemanales,
    boolean calificable,
    boolean activa
) {}
