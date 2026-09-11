package cl.siga.msnotas.model.specifications;

import org.springframework.data.jpa.domain.Specification;

import cl.siga.msnotas.model.entity.Nota;

public class NotaSpecifications {
    public static Specification<Nota> hasIdEstudiante(Long idEstudiante) {
        return (root, query, criteriaBuilder) -> {
            if (idEstudiante == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("idEstudiante"), idEstudiante);
        };
    }

    public static Specification<Nota> hasIdAsignatura(Long idAsignatura) {
        return (root, query, criteriaBuilder) -> {
            if (idAsignatura == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("idAsignatura"), idAsignatura);
        };
    }

    public static Specification<Nota> hasScoreGreaterThanOrEqual(Double score) {
        return (root, query, criteriaBuilder) -> {
            if (score == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("score"), score);
        };
    }

    public static Specification<Nota> hasScoreLessThanOrEqual(Double score) {
        return (root, query, criteriaBuilder) -> {
            if (score == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("score"), score);
        };
    }
}
