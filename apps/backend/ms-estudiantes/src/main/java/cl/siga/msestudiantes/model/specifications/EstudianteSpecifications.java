package cl.siga.msestudiantes.model.specifications;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import cl.siga.msestudiantes.model.entity.Estudiante;

public class EstudianteSpecifications {
    public static Specification<Estudiante> hasRut(String rut) {
        return (root, query, criteriaBuilder) -> {
            if (rut == null || rut.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("rut"), rut.trim());
        };
    }
    
    public static Specification<Estudiante> hasFirstName(String firstName) {
        return (root, query, criteriaBuilder) -> {
            if (firstName == null || firstName.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%" + firstName.trim().toLowerCase() + "%");
        };
    }

    public static Specification<Estudiante> hasMiddleName(String middleName) {
        return (root, query, criteriaBuilder) -> {
            if (middleName == null || middleName.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("middleName")), "%" + middleName.trim().toLowerCase() + "%");
        };
    }

    public static Specification<Estudiante> hasLastName(String lastName) {
        return (root, query, criteriaBuilder) -> {
            if (lastName == null || lastName.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), "%" + lastName.trim().toLowerCase() + "%");
        };
    }

    public static Specification<Estudiante> hasBirthDateGreaterThanOrEqual(LocalDate from) {
        return (root, query, criteriaBuilder) -> {
            if (from == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("birthDate"), from);
        };
    }

    public static Specification<Estudiante> hasBirthDateLessThanOrEqual(LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            if (to == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("birthDate"), to);
        };
    }

    public static Specification<Estudiante> hasState(String state) {
        return (root, query, criteriaBuilder) -> {
            if (state == null || state.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("state"), state.trim());
        };
    }
}
