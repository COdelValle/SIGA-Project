package cl.siga.msasignaturas.model.specifications;

import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msasignaturas.model.entity.Inscripcion;
import cl.siga.msasignaturas.model.entity.asignatura.CursoAsignatura;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class CursoAsignaturaSpecifications {

    public static Specification<CursoAsignatura> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<CursoAsignatura> hasIdClase(Long idClase) {
        return (root, query, cb) -> {
            if (idClase == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("idClase"), idClase);
        };
    }

    public static Specification<CursoAsignatura> hasIdDocente(Long idDocente) {
        return (root, query, cb) -> {
            if (idDocente == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("idDocente"), idDocente);
        };
    }

    public static Specification<CursoAsignatura> hasIdAsignatura(Long idAsignatura) {
        return (root, query, cb) -> {
            if (idAsignatura == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("asignatura").get("id"), idAsignatura);
        };
    }

    public static Specification<CursoAsignatura> hasCaracter(CaracterAsignatura caracter) {
        return (root, query, cb) -> {
            if (caracter == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("caracter"), caracter);
        };
    }

    public static Specification<CursoAsignatura> hasSemestre(Semestre semestre) {
        return (root, query, cb) -> {
            if (semestre == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("semestre"), semestre);
        };
    }

    public static Specification<CursoAsignatura> hasNombre(String nombre) {
        return (root, query, cb) -> {
            if (nombre == null || nombre.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.upper(root.get("asignatura").get("nombre")), "%" + nombre.trim().toUpperCase() + "%");
        };
    }

    public static Specification<CursoAsignatura> hasArea(AreaAcademica area) {
        return (root, query, cb) -> {
            if (area == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("asignatura").get("area"), area);
        };
    }

    /**
     * Dictaciones con cupos disponibles: el cupo máximo supera la cantidad de
     * inscripciones ACTIVO o PRE_INSCRITO.
     */
    public static Specification<CursoAsignatura> hasCuposDisponibles(Boolean conCupoDisponible) {
        return (root, query, cb) -> {
            if (conCupoDisponible == null || !conCupoDisponible) {
                return cb.conjunction();
            }

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Inscripcion> inscripcionRoot = subquery.from(Inscripcion.class);

            subquery.select(cb.count(inscripcionRoot))
                    .where(
                        cb.equal(inscripcionRoot.get("cursoAsignatura"), root),
                        inscripcionRoot.get("estado").in(
                            EstadoInscripcion.ACTIVO,
                            EstadoInscripcion.PRE_INSCRITO
                        )
                    );

            return cb.greaterThan(
                root.get("cupoMaximo").as(Long.class),
                subquery
            );
        };
    }
}
