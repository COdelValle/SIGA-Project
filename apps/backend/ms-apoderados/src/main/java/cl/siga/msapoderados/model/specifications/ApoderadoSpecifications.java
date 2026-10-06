package cl.siga.msapoderados.model.specifications;

import cl.siga.coreshare.format.RutNormalizer;
import cl.siga.msapoderados.model.entity.Apoderado;
import cl.siga.msapoderados.model.entity.ApoderadoEstudiante;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class ApoderadoSpecifications {
  public static Specification<Apoderado> isActivo() {
    return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("activo"));
  }

  public static Specification<Apoderado> hasRut(String rut) {
    return (root, query, criteriaBuilder) -> {
      if (rut == null || rut.trim().isEmpty()) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(root.get("rut"), RutNormalizer.normalizar(rut));
    };
  }

  public static Specification<Apoderado> hasFirstName(String firstName) {
    return (root, query, criteriaBuilder) -> {
      if (firstName == null || firstName.trim().isEmpty()) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%" + firstName.trim().toLowerCase() + "%");
    };
  }

  public static Specification<Apoderado> hasFirstSurname(String firstSurname) {
    return (root, query, criteriaBuilder) -> {
      if (firstSurname == null || firstSurname.trim().isEmpty()) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.like(criteriaBuilder.lower(root.get("firstSurname")), "%" + firstSurname.trim().toLowerCase() + "%");
    };
  }

  public static Specification<Apoderado> hasIdEstudiante(Long idEstudiante) {
    return (root, query, criteriaBuilder) -> {
      if (idEstudiante == null) {
        return criteriaBuilder.conjunction();
      }
      Join<Apoderado, ApoderadoEstudiante> estudiantesJoin = root.join("estudiantes");

      return criteriaBuilder.equal(estudiantesJoin.get("idEstudiante"), idEstudiante);
    };
  }
}
