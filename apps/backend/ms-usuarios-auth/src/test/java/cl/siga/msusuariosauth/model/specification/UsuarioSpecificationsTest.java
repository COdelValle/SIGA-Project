package cl.siga.msusuariosauth.model.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.msusuariosauth.model.entity.Usuario;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

class UsuarioSpecificationsTest {

    @Test
    void sePuedeInstanciarLaClaseDeEspecificaciones() {
        assertThat(new UsuarioSpecifications()).isNotNull();
    }

    @Test
    void hasEmailNormalizaYComparaElCorreo() {
        @SuppressWarnings("unchecked")
        Root<Usuario> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        Path<Object> path = mock(Path.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);
        when(root.get("email")).thenReturn(path);
        when(cb.equal(path, "ana@test.com")).thenReturn(predicate);

        assertThat(UsuarioSpecifications.hasEmail("  Ana@Test.com ")
                .toPredicate(root, null, cb)).isSameAs(predicate);
    }

    @Test
    void hasEmailVacioDevuelveConjuncion() {
        @SuppressWarnings("unchecked")
        Root<Usuario> root = mock(Root.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate conjuncion = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjuncion);

        assertThat(UsuarioSpecifications.hasEmail("  ").toPredicate(root, null, cb))
                .isSameAs(conjuncion);
        assertThat(UsuarioSpecifications.hasEmail(null).toPredicate(root, null, cb))
                .isSameAs(conjuncion);
    }

    @Test
    void hasRolYHasStateComparanCuandoVienenInformados() {
        @SuppressWarnings("unchecked")
        Root<Usuario> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        Path<Object> pathRol = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<Object> pathState = mock(Path.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicateRol = mock(Predicate.class);
        Predicate predicateState = mock(Predicate.class);
        Predicate conjuncion = mock(Predicate.class);
        when(root.get("rol")).thenReturn(pathRol);
        when(root.get("state")).thenReturn(pathState);
        when(cb.equal(pathRol, Rol.DOCENTE)).thenReturn(predicateRol);
        when(cb.equal(pathState, StateUsuario.ACTIVO)).thenReturn(predicateState);
        when(cb.conjunction()).thenReturn(conjuncion);

        assertThat(UsuarioSpecifications.hasRol(Rol.DOCENTE).toPredicate(root, null, cb))
                .isSameAs(predicateRol);
        assertThat(UsuarioSpecifications.hasState(StateUsuario.ACTIVO).toPredicate(root, null, cb))
                .isSameAs(predicateState);
        assertThat(UsuarioSpecifications.hasRol(null).toPredicate(root, null, cb))
                .isSameAs(conjuncion);
        assertThat(UsuarioSpecifications.hasState(null).toPredicate(root, null, cb))
                .isSameAs(conjuncion);
    }

    @Test
    void lasEspecificacionesSePuedenComponer() {
        @SuppressWarnings("unchecked")
        Root<Usuario> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        Path<Object> path = mock(Path.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate base = mock(Predicate.class);
        Predicate filtro = mock(Predicate.class);
        Predicate combinado = mock(Predicate.class);
        when(root.get("email")).thenReturn(path);
        when(cb.equal(path, "ana@test.com")).thenReturn(filtro);
        when(cb.and(filtro, base)).thenReturn(combinado);

        Specification<Usuario> union = UsuarioSpecifications.hasEmail("ana@test.com")
                .and((Specification<Usuario>) (r, q, c) -> base);

        assertThat(union.toPredicate(root, null, cb)).isSameAs(combinado);
        verify(cb).equal(path, "ana@test.com");
    }
}
