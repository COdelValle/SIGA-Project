package cl.siga.msasignaturas.model.specifications;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.msasignaturas.model.entity.Inscripcion;

public class InscripcionSpecifications {

    public static Specification<Inscripcion> hasIdAlumno(Long idAlumno) {
        return (root, query, cb) -> {
            if (idAlumno == null) return cb.conjunction();
            return cb.equal(root.get("idAlumno"), idAlumno);
        };
    }

    public static Specification<Inscripcion> hasIdAsignatura(Long idAsignatura) {
        return (root, query, cb) -> {
            if (idAsignatura == null) return cb.conjunction();
            return cb.equal(root.get("asignatura").get("id"), idAsignatura);
        };
    }

    public static Specification<Inscripcion> hasEstado(EstadoInscripcion estado) {
        return (root, query, cb) -> {
            if (estado == null) return cb.conjunction();
            return cb.equal(root.get("estado"), estado);
        };
    }

    public static Specification<Inscripcion> hasEstadoIn(List<EstadoInscripcion> estados) {
        return (root, query, cb) -> {
            if (estados == null || estados.isEmpty()) return cb.conjunction();
            return root.get("estado").in(estados);
        };
    }
}