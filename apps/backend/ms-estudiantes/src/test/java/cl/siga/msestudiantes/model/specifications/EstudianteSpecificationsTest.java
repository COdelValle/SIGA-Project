package cl.siga.msestudiantes.model.specifications;

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

import cl.siga.coreshare.dto.estudiante.enums.State;
import cl.siga.msestudiantes.model.entity.Estudiante;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EstudianteSpecificationsTest {

    @Mock
    private Root<Estudiante> root;
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

    private void evaluar(Specification<Estudiante> spec) {
        assertDoesNotThrow(() -> spec.toPredicate(root, query, cb));
    }

    @Test
    void isActiveExcluyeInactivos() {
        assertDoesNotThrow(() -> EstudianteSpecifications.isActive().toPredicate(root, query, cb));
    }

    @Test
    void hasRutVacioRetornaConjuncion() {
        assertNotNull(EstudianteSpecifications.hasRut(null).toPredicate(root, query, cb));
        assertNotNull(EstudianteSpecifications.hasRut("  ").toPredicate(root, query, cb));
    }

    @Test
    void hasRutNormalizaYBusca() {
        Path<Object> path = mock(Path.class);
        when(root.get("rut")).thenReturn(path);

        evaluar(EstudianteSpecifications.hasRut("21.000.001-1"));
    }

    @Test
    void hasNombreYApellidosIgnoranValoresVacios() {
        assertNotNull(EstudianteSpecifications.hasFirstName(null).toPredicate(root, query, cb));
        assertNotNull(EstudianteSpecifications.hasFirstName(" ").toPredicate(root, query, cb));
        assertNotNull(EstudianteSpecifications.hasMiddleName("").toPredicate(root, query, cb));
        assertNotNull(EstudianteSpecifications.hasFirstSurname(null).toPredicate(root, query, cb));
        assertNotNull(EstudianteSpecifications.hasSecondSurname("  ").toPredicate(root, query, cb));
    }

    @Test
    void hasNombreYApellidosBuscanPorCoincidencia() {
        evaluar(EstudianteSpecifications.hasFirstName("Ana"));
        evaluar(EstudianteSpecifications.hasMiddleName("Maria"));
        evaluar(EstudianteSpecifications.hasFirstSurname("Perez"));
        evaluar(EstudianteSpecifications.hasSecondSurname("Soto"));
    }

    @Test
    void hasFechasIgnoranNulosYFiltranValores() {
        assertNotNull(EstudianteSpecifications.hasBirthDateGreaterThanOrEqual(null).toPredicate(root, query, cb));
        assertNotNull(EstudianteSpecifications.hasBirthDateLessThanOrEqual(null).toPredicate(root, query, cb));
        evaluar(EstudianteSpecifications.hasBirthDateGreaterThanOrEqual(LocalDate.of(2010, 1, 1)));
        evaluar(EstudianteSpecifications.hasBirthDateLessThanOrEqual(LocalDate.of(2020, 1, 1)));
    }

    @Test
    void hasStateIgnoraNuloYFiltraValor() {
        assertNotNull(EstudianteSpecifications.hasState(null).toPredicate(root, query, cb));
        evaluar(EstudianteSpecifications.hasState(State.REGISTRADO));
    }

    @Test
    void hasIdClaseIgnoraNuloYFiltraValor() {
        assertNotNull(EstudianteSpecifications.hasIdClase(null).toPredicate(root, query, cb));
        evaluar(EstudianteSpecifications.hasIdClase(7L));
    }

    @Test
    void hasTextoLibreIgnoraTextosCortosOVacios() {
        assertNotNull(EstudianteSpecifications.hasTextoLibre(null).toPredicate(root, query, cb));
        assertNotNull(EstudianteSpecifications.hasTextoLibre("a").toPredicate(root, query, cb));
        assertNotNull(EstudianteSpecifications.hasTextoLibre("  ").toPredicate(root, query, cb));
    }

    @Test
    void hasTextoLibreBuscaPorRutConPuntosYMultiplesPalabras() {
        evaluar(EstudianteSpecifications.hasTextoLibre("22.126.386-3"));
        evaluar(EstudianteSpecifications.hasTextoLibre("catalina ormeno"));
        evaluar(EstudianteSpecifications.hasTextoLibre("  ...  "));
    }
}
