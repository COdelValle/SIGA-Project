package cl.siga.msevaluaciones.model.specifications;

import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import cl.siga.msevaluaciones.model.entity.Evaluacion;
import org.springframework.data.jpa.domain.Specification;

public class EvaluacionSpecifications {

  // Filtro para obtener solo las evaluaciones que no han sido eliminadas lógicamente.
  public static Specification<Evaluacion> isActive() {
    return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("active"));
  }

  // Filtro por nombre (búsqueda exacta o parcial dependiendo de tu necesidad, aquí uso coincidencia parcial ignorando mayúsculas/minúsculas).
  public static Specification<Evaluacion> hasNombre(String nombre) {
    return (root, query, criteriaBuilder) -> {
      if (nombre == null || nombre.trim().isEmpty()) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.like(
        criteriaBuilder.upper(root.get("nombre")),
        "%" + nombre.trim().toUpperCase() + "%"
      );
    };
  }

  // Filtro exacto por el tipo de evaluación (FORMATIVA, DIAGNOSTICO, SUMATIVA).
  public static Specification<Evaluacion> hasTipo(TipoEvaluacion tipo) {
    return (root, query, criteriaBuilder) -> {
      if (tipo == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(root.get("tipo"), tipo);
    };
  }

  // Filtro para obtener todas las evaluaciones que pertenecen a una asignatura en específico.
  public static Specification<Evaluacion> hasIdAsignatura(Long idAsignatura) {
    return (root, query, criteriaBuilder) -> {
      if (idAsignatura == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(root.get("idAsignatura"), idAsignatura);
    };
  }
}
