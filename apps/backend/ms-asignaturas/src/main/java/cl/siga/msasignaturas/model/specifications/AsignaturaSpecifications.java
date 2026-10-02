package cl.siga.msasignaturas.model.specifications;

import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.enums.TipoAsignatura;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msasignaturas.model.entity.Inscripcion;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import cl.siga.msasignaturas.model.entity.asignatura.AsignaturaBasica;
import cl.siga.msasignaturas.model.entity.asignatura.AsignaturaElectiva;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class AsignaturaSpecifications {

    // ==========================================
    // 1. Filtros Generales (Clase Padre: Asignatura)
    // ==========================================

    /**
     * Filtra solo asignaturas activas (Borrado Lógico).
     */
    public static Specification<Asignatura> isActive() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("active"));
    }

    /**
     * Búsqueda por nombre parcial (insensible a mayúsculas/minúsculas).
     */
    public static Specification<Asignatura> hasName(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.upper(root.get("name")),
                "%" + name.trim().toUpperCase() + "%"
            );
        };
    }

    /**
     * Filtra por Semestre (SEMESTRE_1, SEMESTRE_2).
     */
    public static Specification<Asignatura> hasSemestre(Semestre semestre) {
        return (root, query, criteriaBuilder) -> {
            if (semestre == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("semestre"), semestre);
        };
    }

    /**
     * Filtra por Área Académica (MATEMATICAS, CIENCIAS, LENGUAJE, etc.).
     */
    public static Specification<Asignatura> hasArea(AreaAcademica area) {
        return (root, query, criteriaBuilder) -> {
            if (area == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("area"), area);
        };
    }

    /**
     * Filtra por el ID del docente asignado.
     */
    public static Specification<Asignatura> hasIdDocente(Long idDocente) {
        return (root, query, criteriaBuilder) -> {
            if (idDocente == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("idDocente"), idDocente);
        };
    }

    /**
     * Filtra por el tipo de discriminador (BASICA o ELECTIVA).
     */
    public static Specification<Asignatura> hasTipo(TipoAsignatura tipo) {
        return (root, query, criteriaBuilder) -> {
            if (tipo == null) {
                return criteriaBuilder.conjunction();
            }
            if (TipoAsignatura.BASICA == tipo) {
                return criteriaBuilder.equal(root.type(), AsignaturaBasica.class);
            } else if (TipoAsignatura.ELECTIVA == tipo) {
                return criteriaBuilder.equal(root.type(), AsignaturaElectiva.class);
            }
            return criteriaBuilder.conjunction();
        };
    }

    // ==========================================
    // 2. Filtros Específicos para AsignaturaBasica
    // ==========================================

    /**
     * Filtra asignaturas básicas por el ID de la clase/curso (ej: ver todas las asignaturas de 3° Medio A).
     * Hace downcast seguro a AsignaturaBasica mediante 'treat()'.
     */
    public static Specification<Asignatura> hasIdClase(Long idClase) {
        return (root, query, criteriaBuilder) -> {
            if (idClase == null) {
                return criteriaBuilder.conjunction();
            }
            Root<AsignaturaBasica> basicaRoot = criteriaBuilder.treat(root, AsignaturaBasica.class);
            return criteriaBuilder.equal(basicaRoot.get("idClase"), idClase);
        };
    }

    // ==========================================
    // 3. Filtros Específicos para AsignaturaElectiva
    // ==========================================

    /**
     * Filtra asignaturas electivas que posean cupos disponibles.
     * Evalúa si el cupo máximo es estrictamente mayor a la cantidad de 
     * inscripciones actuales en estado ACTIVO o PRE_INSCRITO.
     */
    public static Specification<Asignatura> hasCuposDisponibles(Boolean verificarCupos) {
        return (root, query, criteriaBuilder) -> {
            if (verificarCupos == null || !verificarCupos) {
                return criteriaBuilder.conjunction();
            }

            // Downcast a la entidad hija
            Root<AsignaturaElectiva> electivaRoot = criteriaBuilder.treat(root, AsignaturaElectiva.class);

            // Creamos una subquery para contar las inscripciones válidas de esta asignatura
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Inscripcion> inscripcionRoot = subquery.from(Inscripcion.class);
            
            subquery.select(criteriaBuilder.count(inscripcionRoot))
                    .where(
                        criteriaBuilder.equal(inscripcionRoot.get("asignatura"), electivaRoot),
                        inscripcionRoot.get("estado").in(
                            EstadoInscripcion.ACTIVO, 
                            EstadoInscripcion.PRE_INSCRITO
                        )
                    );

            // Condición: cupoMaximo > (cantidad de inscripciones válidas obtenidas en la subquery)
            // Casteamos cupoMaximo a Long para que coincida con el tipo de retorno de la subquery (count)
            return criteriaBuilder.greaterThan(
                electivaRoot.get("cupoMaximo").as(Long.class),
                subquery
            );
        };
    }
}