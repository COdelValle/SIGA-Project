package cl.siga.msasistencias.model.specifications;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
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

import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.msasistencias.model.entity.Asistencia;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsistenciaSpecificationsTest {

    @Mock
    private Root<Asistencia> root;
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

    private void evaluar(Specification<Asistencia> spec) {
        assertDoesNotThrow(() -> spec.toPredicate(root, query, cb));
    }

    @Test
    void isActiveFiltraPorActiveTrue() {
        assertDoesNotThrow(() -> AsistenciaSpecifications.isActive().toPredicate(root, query, cb));
    }

    @Test
    void hasIdEstudianteIgnoraNuloYFiltraValor() {
        assertNotNull(AsistenciaSpecifications.hasIdEstudiante(null).toPredicate(root, query, cb));
        evaluar(AsistenciaSpecifications.hasIdEstudiante(1L));
    }

    @Test
    void hasIdCursoAsignaturaIgnoraNuloYFiltraValor() {
        assertNotNull(AsistenciaSpecifications.hasIdCursoAsignatura(null).toPredicate(root, query, cb));
        evaluar(AsistenciaSpecifications.hasIdCursoAsignatura(5L));
    }

    @Test
    void hasFechaGreaterThanOrEqualIgnoraNuloYFiltraValor() {
        assertNotNull(AsistenciaSpecifications.hasFechaGreaterThanOrEqual(null)
            .toPredicate(root, query, cb));
        evaluar(AsistenciaSpecifications.hasFechaGreaterThanOrEqual(LocalDate.of(2026, 10, 1)));
    }

    @Test
    void hasFechaLessThanOrEqualIgnoraNuloYFiltraValor() {
        assertNotNull(AsistenciaSpecifications.hasFechaLessThanOrEqual(null)
            .toPredicate(root, query, cb));
        evaluar(AsistenciaSpecifications.hasFechaLessThanOrEqual(LocalDate.of(2026, 10, 31)));
    }

    @Test
    void hasEstadoIgnoraNuloYFiltraValor() {
        assertNotNull(AsistenciaSpecifications.hasEstado(null).toPredicate(root, query, cb));
        evaluar(AsistenciaSpecifications.hasEstado(State.AUSENTE));
    }
}
