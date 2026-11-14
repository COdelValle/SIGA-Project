package cl.siga.msnotas.model.specifications;

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

import cl.siga.msnotas.model.entity.Nota;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotaSpecificationsTest {

    @Mock
    private Root<Nota> root;
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

    private void evaluar(Specification<Nota> spec) {
        assertDoesNotThrow(() -> spec.toPredicate(root, query, cb));
    }

    @Test
    void isActiveFiltraPorActiveTrue() {
        assertDoesNotThrow(() -> NotaSpecifications.isActive().toPredicate(root, query, cb));
    }

    @Test
    void hasIdEstudianteIgnoraNuloYFiltraValor() {
        assertNotNull(NotaSpecifications.hasIdEstudiante(null).toPredicate(root, query, cb));
        evaluar(NotaSpecifications.hasIdEstudiante(1L));
    }

    @Test
    void hasIdEvaluacionIgnoraNuloYFiltraValor() {
        assertNotNull(NotaSpecifications.hasIdEvaluacion(null).toPredicate(root, query, cb));
        evaluar(NotaSpecifications.hasIdEvaluacion(9L));
    }

    @Test
    void hasScoreGreaterThanOrEqualIgnoraNuloYFiltraValor() {
        assertNotNull(NotaSpecifications.hasScoreGreaterThanOrEqual(null).toPredicate(root, query, cb));
        evaluar(NotaSpecifications.hasScoreGreaterThanOrEqual(4.0));
    }

    @Test
    void hasScoreLessThanOrEqualIgnoraNuloYFiltraValor() {
        assertNotNull(NotaSpecifications.hasScoreLessThanOrEqual(null).toPredicate(root, query, cb));
        evaluar(NotaSpecifications.hasScoreLessThanOrEqual(7.0));
    }
}
