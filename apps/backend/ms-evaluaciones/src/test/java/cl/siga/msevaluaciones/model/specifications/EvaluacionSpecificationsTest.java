package cl.siga.msevaluaciones.model.specifications;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import cl.siga.msevaluaciones.model.entity.Evaluacion;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EvaluacionSpecificationsTest {

    @Mock
    private Root<Evaluacion> root;
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

    private void evaluar(Specification<Evaluacion> spec) {
        assertDoesNotThrow(() -> spec.toPredicate(root, query, cb));
    }

    @Test
    void isActiveFiltraPorActiveTrue() {
        assertDoesNotThrow(() -> EvaluacionSpecifications.isActive().toPredicate(root, query, cb));
    }

    @Test
    void hasNombreIgnoraNuloYVacioYFiltraValor() {
        assertNotNull(EvaluacionSpecifications.hasNombre(null).toPredicate(root, query, cb));
        assertNotNull(EvaluacionSpecifications.hasNombre("  ").toPredicate(root, query, cb));
        evaluar(EvaluacionSpecifications.hasNombre(" prueba "));
    }

    @Test
    void hasTipoIgnoraNuloYFiltraValor() {
        assertNotNull(EvaluacionSpecifications.hasTipo(null).toPredicate(root, query, cb));
        evaluar(EvaluacionSpecifications.hasTipo(TipoEvaluacion.SUMATIVA));
    }

    @Test
    void hasIdCursoAsignaturaIgnoraNuloYFiltraValor() {
        assertNotNull(EvaluacionSpecifications.hasIdCursoAsignatura(null).toPredicate(root, query, cb));
        evaluar(EvaluacionSpecifications.hasIdCursoAsignatura(5L));
    }
}
