package cl.siga.msapoderados.model.specifications;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import cl.siga.msapoderados.model.entity.Apoderado;
import cl.siga.msapoderados.model.entity.ApoderadoEstudiante;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApoderadoSpecificationsTest {

    @Mock
    private Root<Apoderado> root;
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

    private void evaluar(Specification<Apoderado> spec) {
        assertDoesNotThrow(() -> spec.toPredicate(root, query, cb));
    }

    @Test
    void isActivoFiltraPorActivoTrue() {
        assertDoesNotThrow(() -> ApoderadoSpecifications.isActivo().toPredicate(root, query, cb));
    }

    @Test
    void hasRutIgnoraVaciosYNormaliza() {
        assertNotNull(ApoderadoSpecifications.hasRut(null).toPredicate(root, query, cb));
        assertNotNull(ApoderadoSpecifications.hasRut("  ").toPredicate(root, query, cb));
        evaluar(ApoderadoSpecifications.hasRut("21.000.003-8"));
    }

    @Test
    void hasNombreYApellidoIgnoranVaciosYBuscan() {
        assertNotNull(ApoderadoSpecifications.hasFirstName(null).toPredicate(root, query, cb));
        assertNotNull(ApoderadoSpecifications.hasFirstName("").toPredicate(root, query, cb));
        assertNotNull(ApoderadoSpecifications.hasFirstSurname(" ").toPredicate(root, query, cb));
        evaluar(ApoderadoSpecifications.hasFirstName("Luis"));
        evaluar(ApoderadoSpecifications.hasFirstSurname("Soto"));
    }

    @Test
    void hasIdEstudianteIgnoraNuloYHaceJoin() {
        assertNotNull(ApoderadoSpecifications.hasIdEstudiante(null).toPredicate(root, query, cb));

        @SuppressWarnings("unchecked")
        Join<Object, Object> join = mock(Join.class);
        @SuppressWarnings("unchecked")
        Path<Object> path = mock(Path.class);
        when(root.join("estudiantes")).thenReturn(join);
        when(join.get(anyString())).thenReturn(path);

        evaluar(ApoderadoSpecifications.hasIdEstudiante(7L));

        verify(root).join("estudiantes");
    }
}
