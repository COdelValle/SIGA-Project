package cl.siga.msasignaturas.model.specifications;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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

import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsignaturaSpecificationsTest {

    @Mock
    private Root<Asignatura> root;
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

    private void evaluar(Specification<Asignatura> spec) {
        assertDoesNotThrow(() -> spec.toPredicate(root, query, cb));
    }

    @Test
    void isActiveFiltraPorActiveTrue() {
        assertDoesNotThrow(() -> AsignaturaSpecifications.isActive().toPredicate(root, query, cb));
    }

    @Test
    void hasNombreIgnoraVaciosYBusca() {
        assertNotNull(AsignaturaSpecifications.hasNombre(null).toPredicate(root, query, cb));
        assertNotNull(AsignaturaSpecifications.hasNombre(" ").toPredicate(root, query, cb));
        evaluar(AsignaturaSpecifications.hasNombre("mat"));
    }

    @Test
    void hasAreaIgnoraNuloYFiltraValor() {
        assertNotNull(AsignaturaSpecifications.hasArea(null).toPredicate(root, query, cb));
        evaluar(AsignaturaSpecifications.hasArea(AreaAcademica.MATEMATICAS));
    }

    @Test
    void hasCalificableIgnoraNuloYFiltraValor() {
        assertNotNull(AsignaturaSpecifications.hasCalificable(null).toPredicate(root, query, cb));
        evaluar(AsignaturaSpecifications.hasCalificable(true));
        evaluar(AsignaturaSpecifications.hasCalificable(false));
    }
}
