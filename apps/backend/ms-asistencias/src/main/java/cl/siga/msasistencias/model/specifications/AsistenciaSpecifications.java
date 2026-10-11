package cl.siga.msasistencias.model.specifications;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.msasistencias.model.entity.Asistencia;

public class AsistenciaSpecifications {
    private AsistenciaSpecifications() {
    }

    public static Specification<Asistencia> isActive() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("active"));
    }

    public static Specification<Asistencia> hasIdEstudiante(Long idEstudiante) {
        return (root, query, criteriaBuilder) -> {
            if (idEstudiante == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("idEstudiante"), idEstudiante);
        };
    }

    public static Specification<Asistencia> hasIdCursoAsignatura(Long idCursoAsignatura) {
        return (root, query, criteriaBuilder) -> {
            if (idCursoAsignatura == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("idCursoAsignatura"), idCursoAsignatura);
        };
    }

    public static Specification<Asistencia> hasFechaGreaterThanOrEqual(LocalDate from) {
        return (root, query, criteriaBuilder) -> {
            if (from == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("fecha"), from);
        };
    }

    public static Specification<Asistencia> hasFechaLessThanOrEqual(LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            if (to == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("fecha"), to);
        };
    }

    public static Specification<Asistencia> hasEstado(State estado) {
        return (root, query, criteriaBuilder) -> {
            if (estado == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("estado"), estado);
        };
    }
}
