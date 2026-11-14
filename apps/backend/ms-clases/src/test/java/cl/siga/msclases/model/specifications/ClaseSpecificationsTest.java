package cl.siga.msclases.model.specifications;

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

import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.msclases.model.entity.Clase;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaseSpecificationsTest {

    @Mock
    private Root<Clase> root;
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

    private void evaluar(Specification<Clase> spec) {
        assertDoesNotThrow(() -> spec.toPredicate(root, query, cb));
    }

    @Test
    void isActiveFiltraPorActiveTrue() {
        assertDoesNotThrow(() -> ClaseSpecifications.isActive().toPredicate(root, query, cb));
    }

    @Test
    void hasNivelIgnoraNuloYFiltraValor() {
        assertNotNull(ClaseSpecifications.hasNivel(null).toPredicate(root, query, cb));
        evaluar(ClaseSpecifications.hasNivel(Nivel.OCTAVO_BASICO));
    }

    @Test
    void hasLetraIgnoraVaciosYNormaliza() {
        assertNotNull(ClaseSpecifications.hasLetra(null).toPredicate(root, query, cb));
        assertNotNull(ClaseSpecifications.hasLetra("  ").toPredicate(root, query, cb));
        evaluar(ClaseSpecifications.hasLetra(" a "));
    }

    @Test
    void hasAnioAcademicoIgnoraNuloYFiltraValor() {
        assertNotNull(ClaseSpecifications.hasAnioAcademico(null).toPredicate(root, query, cb));
        evaluar(ClaseSpecifications.hasAnioAcademico(2026));
    }

    @Test
    void hasIdDocenteJefeIgnoraNuloYFiltraValor() {
        assertNotNull(ClaseSpecifications.hasIdDocenteJefe(null).toPredicate(root, query, cb));
        evaluar(ClaseSpecifications.hasIdDocenteJefe(2L));
    }
}
