package cl.siga.msestudiantes.model.specifications;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.dto.estudiante.enums.State;
import cl.siga.coreshare.format.RutNormalizer;
import cl.siga.msestudiantes.model.entity.Estudiante;

import jakarta.persistence.criteria.Predicate;

public class EstudianteSpecifications {
    public static Specification<Estudiante> isActive() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.notEqual(root.get("state"), State.INACTIVO);
    }

    public static Specification<Estudiante> hasRut(String rut) {
        return (root, query, criteriaBuilder) -> {
            if (rut == null || rut.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("rut"), RutNormalizer.normalizar(rut));
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

    public static Specification<Estudiante> hasFirstSurname(String firstSurname) {
        return (root, query, criteriaBuilder) -> {
            if (firstSurname == null || firstSurname.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("firstSurname")), "%" + firstSurname.trim().toLowerCase() + "%");
        };
    }

    public static Specification<Estudiante> hasSecondSurname(String secondSurname) {
        return (root, query, criteriaBuilder) -> {
            if (secondSurname == null || secondSurname.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("secondSurname")), "%" + secondSurname.trim().toLowerCase() + "%");
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

    public static Specification<Estudiante> hasState(State state) {
        return (root, query, criteriaBuilder) -> {
            if (state == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("state"), state);
        };
    }

    public static Specification<Estudiante> hasIdClase(Long idClase) {
        return (root, query, criteriaBuilder) -> {
            if (idClase == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("idClase"), idClase);
        };
    }

    /**
     * Busqueda libre para el selector de alumnos: cada palabra debe coincidir con
     * el RUT (con o sin puntos) o con nombres/apellidos. Permite "22.126.386-3",
     * "22126386" y "catalina ormeno".
     */
    public static Specification<Estudiante> hasTextoLibre(String texto) {
        return (root, query, criteriaBuilder) -> {
            if (texto == null || texto.trim().length() < 2) {
                return criteriaBuilder.conjunction();
            }
            String[] tokens = texto.trim().toLowerCase().split("\\s+");
            List<Predicate> porToken = new ArrayList<>();
            for (String token : tokens) {
                String limpio = token.replace(".", "").trim();
                if (limpio.isEmpty()) {
                    continue;
                }
                String like = "%" + limpio + "%";
                porToken.add(criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(criteriaBuilder.function(
                                        "replace", String.class, root.get("rut"),
                                        criteriaBuilder.literal("."), criteriaBuilder.literal(""))),
                                like),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), like),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("middleName")), like),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstSurname")), like),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("secondSurname")), like)));
            }
            if (porToken.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.and(porToken.toArray(new Predicate[0]));
        };
    }
}
