package cl.siga.msasignaturas.model.specifications;

import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;

public class AsignaturaSpecifications {

    public static Specification<Asignatura> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Asignatura> hasNombre(String nombre) {
        return (root, query, cb) -> {
            if (nombre == null || nombre.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.upper(root.get("nombre")), "%" + nombre.trim().toUpperCase() + "%");
        };
    }

    public static Specification<Asignatura> hasArea(AreaAcademica area) {
        return (root, query, cb) -> {
            if (area == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("area"), area);
        };
    }

    public static Specification<Asignatura> hasCalificable(Boolean calificable) {
        return (root, query, cb) -> {
            if (calificable == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("calificable"), calificable);
        };
    }
}
