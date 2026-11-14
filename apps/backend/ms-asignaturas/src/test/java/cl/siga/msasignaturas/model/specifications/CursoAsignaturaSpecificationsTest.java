package cl.siga.msasignaturas.model.specifications;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msasignaturas.model.entity.Inscripcion;
import cl.siga.msasignaturas.model.entity.asignatura.CursoAsignatura;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CursoAsignaturaSpecificationsTest {

    @Mock
    private Root<CursoAsignatura> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;

    private Predicate conjunction;

    @BeforeEach
    void setUp() {
        conjunction = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjunction);
    }

    private void evaluar(Specification<CursoAsignatura> spec) {
        assertDoesNotThrow(() -> spec.toPredicate(root, query, cb));
    }

    @Test
    void isActiveFiltraPorActiveTrue() {
        assertDoesNotThrow(() -> CursoAsignaturaSpecifications.isActive().toPredicate(root, query, cb));
    }

    @Test
    void filtrosSimplesIgnoranNulosYFiltranValores() {
        assertNotNull(CursoAsignaturaSpecifications.hasIdClase(null).toPredicate(root, query, cb));
        assertNotNull(CursoAsignaturaSpecifications.hasIdDocente(null).toPredicate(root, query, cb));
        assertNotNull(CursoAsignaturaSpecifications.hasIdAsignatura(null).toPredicate(root, query, cb));
        assertNotNull(CursoAsignaturaSpecifications.hasCaracter(null).toPredicate(root, query, cb));
        assertNotNull(CursoAsignaturaSpecifications.hasSemestre(null).toPredicate(root, query, cb));
        assertNotNull(CursoAsignaturaSpecifications.hasNombre(null).toPredicate(root, query, cb));
        assertNotNull(CursoAsignaturaSpecifications.hasNombre(" ").toPredicate(root, query, cb));
        assertNotNull(CursoAsignaturaSpecifications.hasArea(null).toPredicate(root, query, cb));

        evaluar(CursoAsignaturaSpecifications.hasIdClase(5L));
        evaluar(CursoAsignaturaSpecifications.hasIdDocente(2L));
        evaluar(CursoAsignaturaSpecifications.hasCaracter(CaracterAsignatura.OBLIGATORIA));
        evaluar(CursoAsignaturaSpecifications.hasSemestre(Semestre.SEMESTRE_1));
    }

    @Test
    void filtrosConAsignaturaNaveganLaRelacion() {
        Path<Object> asignaturaPath = mock(Path.class);
        Path<Object> path = mock(Path.class);
        when(root.get("asignatura")).thenReturn(asignaturaPath);
        when(asignaturaPath.get(anyString())).thenReturn(path);

        evaluar(CursoAsignaturaSpecifications.hasIdAsignatura(1L));
        evaluar(CursoAsignaturaSpecifications.hasNombre("mat"));
        evaluar(CursoAsignaturaSpecifications.hasArea(AreaAcademica.MATEMATICAS));
    }

    @Test
    void hasCuposDisponiblesIgnoraFalsoONulo() {
        assertNotNull(CursoAsignaturaSpecifications.hasCuposDisponibles(null).toPredicate(root, query, cb));
        assertNotNull(CursoAsignaturaSpecifications.hasCuposDisponibles(false).toPredicate(root, query, cb));
    }

    @Test
    void hasCuposDisponiblesConstruyeSubquery() {
        @SuppressWarnings("rawtypes")
        Path path = mock(Path.class);
        when(path.get(anyString())).thenReturn(path);
        when(path.as(any())).thenReturn(path);
        when(root.get(anyString())).thenReturn(path);
        when(root.get("cupoMaximo")).thenReturn(path);

        @SuppressWarnings("unchecked")
        Subquery<Long> subquery = mock(Subquery.class);
        @SuppressWarnings("unchecked")
        Root<Inscripcion> inscripcionRoot = mock(Root.class);
        when(query.subquery(Long.class)).thenReturn(subquery);
        when(subquery.from(Inscripcion.class)).thenReturn(inscripcionRoot);
        when(subquery.select(nullable(jakarta.persistence.criteria.Expression.class))).thenReturn(subquery);
        when(inscripcionRoot.get(anyString())).thenReturn(path);

        evaluar(CursoAsignaturaSpecifications.hasCuposDisponibles(true));

        verify(query).subquery(Long.class);
        verify(subquery).from(Inscripcion.class);
    }
}
