package cl.siga.msasignaturas.model.specifications;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.msasignaturas.model.entity.Inscripcion;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InscripcionSpecificationsTest {

    @Mock
    private Root<Inscripcion> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;

    private Predicate conjunction;
    private Predicate predicado;

    @BeforeEach
    void setUp() {
        conjunction = mock(Predicate.class);
        predicado = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjunction);
        when(cb.equal(any(), any())).thenReturn(predicado);
    }

    private Predicate evaluar(Specification<Inscripcion> spec) {
        return spec.toPredicate(root, query, cb);
    }

    @Test
    void laClaseSePuedeInstanciarParaCubrirSuConstructor() {
        assertNotNull(new InscripcionSpecifications());
    }

    @Test
    void hasIdAlumnoIgnoraNuloYFiltraElValor() {
        assertNotNull(evaluar(InscripcionSpecifications.hasIdAlumno(null)));

        Path<Object> path = mock(Path.class);
        when(root.get("idAlumno")).thenReturn(path);

        evaluar(InscripcionSpecifications.hasIdAlumno(10L));
        verify(cb).equal(path, 10L);
    }

    @Test
    void hasIdCursoAsignaturaIgnoraNuloYNavegaLaRelacion() {
        assertNotNull(evaluar(InscripcionSpecifications.hasIdCursoAsignatura(null)));

        Path<Object> cursoPath = mock(Path.class);
        Path<Object> idPath = mock(Path.class);
        when(root.get("cursoAsignatura")).thenReturn(cursoPath);
        when(cursoPath.get("id")).thenReturn(idPath);

        evaluar(InscripcionSpecifications.hasIdCursoAsignatura(20L));
        verify(cb).equal(idPath, 20L);
    }

    @Test
    void hasEstadoIgnoraNuloYFiltraElValor() {
        assertNotNull(evaluar(InscripcionSpecifications.hasEstado(null)));

        Path<Object> path = mock(Path.class);
        when(root.get("estado")).thenReturn(path);

        evaluar(InscripcionSpecifications.hasEstado(EstadoInscripcion.ACTIVO));
        verify(cb).equal(path, EstadoInscripcion.ACTIVO);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void hasEstadoInIgnoraListasVaciasYFiltraLosEstados() {
        assertNotNull(evaluar(InscripcionSpecifications.hasEstadoIn(null)));
        assertNotNull(evaluar(InscripcionSpecifications.hasEstadoIn(List.of())));

        Path path = mock(Path.class);
        when(root.get(anyString())).thenReturn(path);
        when(path.in(any(List.class))).thenReturn(predicado);

        List<EstadoInscripcion> estados = List.of(EstadoInscripcion.ACTIVO, EstadoInscripcion.PRE_INSCRITO);
        assertNotNull(evaluar(InscripcionSpecifications.hasEstadoIn(estados)));
        verify(path).in(estados);
    }
}
