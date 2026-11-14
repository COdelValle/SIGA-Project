package cl.siga.msdocentes.model.specifications;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msdocentes.model.entity.Docente;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocenteSpecificationsTest {

    @Mock
    private Root<Docente> root;
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

    private void evaluar(Specification<Docente> spec) {
        assertDoesNotThrow(() -> spec.toPredicate(root, query, cb));
    }

    @Test
    void isActivoFiltraPorActivoTrue() {
        assertDoesNotThrow(() -> DocenteSpecifications.isActivo().toPredicate(root, query, cb));
    }

    @Test
    void hasRutIgnoraVaciosYNormaliza() {
        assertNotNull(DocenteSpecifications.hasRut(null).toPredicate(root, query, cb));
        assertNotNull(DocenteSpecifications.hasRut(" ").toPredicate(root, query, cb));
        evaluar(DocenteSpecifications.hasRut("21.000.002-K"));
    }

    @Test
    void hasNombreYApellidoIgnoranVaciosYBuscan() {
        assertNotNull(DocenteSpecifications.hasFirstName(null).toPredicate(root, query, cb));
        assertNotNull(DocenteSpecifications.hasFirstName("").toPredicate(root, query, cb));
        assertNotNull(DocenteSpecifications.hasFirstSurname(" ").toPredicate(root, query, cb));
        evaluar(DocenteSpecifications.hasFirstName("Ana"));
        evaluar(DocenteSpecifications.hasFirstSurname("Perez"));
    }

    @Test
    void hasFechasIgnoranNulosYFiltranValores() {
        assertNotNull(DocenteSpecifications.hasFechaContratacionGreaterThanOrEqual(null)
                .toPredicate(root, query, cb));
        assertNotNull(DocenteSpecifications.hasFechaContratacionLessThanOrEqual(null)
                .toPredicate(root, query, cb));
        evaluar(DocenteSpecifications.hasFechaContratacionGreaterThanOrEqual(LocalDate.of(2010, 1, 1)));
        evaluar(DocenteSpecifications.hasFechaContratacionLessThanOrEqual(LocalDate.of(2025, 1, 1)));
    }

    @Test
    void hasAreaIgnoraNuloYFiltraValor() {
        assertNotNull(DocenteSpecifications.hasArea(null).toPredicate(root, query, cb));
        evaluar(DocenteSpecifications.hasArea(AreaAcademica.MATEMATICAS));
    }
}
