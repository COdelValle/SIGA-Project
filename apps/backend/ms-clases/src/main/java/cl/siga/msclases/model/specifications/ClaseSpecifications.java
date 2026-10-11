package cl.siga.msclases.model.specifications;

import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.msclases.model.entity.Clase;
import org.springframework.data.jpa.domain.Specification;

public class ClaseSpecifications {
  public static Specification<Clase> isActive() {
    return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("active"));
  }

  public static Specification<Clase> hasNivel(Nivel nivel) {
    return (root, query, criteriaBuilder) -> {
      if (nivel == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(root.get("nivel"), nivel);
    };
  }

  public static Specification<Clase> hasLetra(String letra) {
    return (root, query, criteriaBuilder) -> {
      if (letra == null || letra.trim().isEmpty()) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(
        criteriaBuilder.upper(root.get("letra")),
        letra.trim().toUpperCase()
      );
    };
  }

  public static Specification<Clase> hasAnioAcademico(Integer anioAcademico) {
    return (root, query, criteriaBuilder) -> {
      if (anioAcademico == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(root.get("anioAcademico"), anioAcademico);
    };
  }

  public static Specification<Clase> hasIdDocenteJefe(Long idDocenteJefe) {
    return (root, query, criteriaBuilder) -> {
      if (idDocenteJefe == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(root.get("idDocenteJefe"), idDocenteJefe);
    };
  }
}
