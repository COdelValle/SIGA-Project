package cl.siga.msusuariosauth.model.specification;

import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.msusuariosauth.model.entity.Usuario;

public class UsuarioSpecifications {
    public static Specification<Usuario> hasEmail(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("email"), email.trim().toLowerCase());
        };
    }

    public static Specification<Usuario> hasRol(Rol rol) {
        return (root, query, criteriaBuilder) -> 
            rol == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("rol"), rol);
    }

    public static Specification<Usuario> hasState(StateUsuario state) {
        return (root, query, criteriaBuilder) -> 
            state == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("state"), state);
    }
}
