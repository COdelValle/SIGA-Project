package cl.siga.msdocentes.model.specifications;

import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msdocentes.model.entity.Docente;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class DocenteSpecifications {
  public static Specification<Docente> isActivo() {
    return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("activo"));
  }

  public static Specification<Docente> hasRut(String rut) {
    return (root, query, criteriaBuilder) -> {
      if (rut == null || rut.trim().isEmpty()) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(root.get("rut"), rut.trim().toUpperCase());
    };
  }

  public static Specification<Docente> hasFirstName(String firstName) {
    return (root, query, criteriaBuilder) -> {
      if (firstName == null || firstName.trim().isEmpty()) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%" + firstName.trim().toLowerCase() + "%");
    };
  }

  public static Specification<Docente> hasFirstSurname(String firstSurname) {
    return (root, query, criteriaBuilder) -> {
      if (firstSurname == null || firstSurname.trim().isEmpty()) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.like(criteriaBuilder.lower(root.get("firstSurname")), "%" + firstSurname.trim().toLowerCase() + "%");
    };
  }

  public static Specification<Docente> hasFechaContratacionGreaterThanOrEqual(LocalDate from) {
    return (root, query, criteriaBuilder) -> {
      if (from == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.greaterThanOrEqualTo(root.get("fechaContratacion"), from);
    };
  }

  public static Specification<Docente> hasFechaContratacionLessThanOrEqual(LocalDate to) {
    return (root, query, criteriaBuilder) -> {
      if (to == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.lessThanOrEqualTo(root.get("fechaContratacion"), to);
    };
  }

  public static Specification<Docente> hasArea(AreaAcademica area) {
    return (root, query, criteriaBuilder) -> {
      if (area == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(root.get("area"), area);
    };
  }
}
