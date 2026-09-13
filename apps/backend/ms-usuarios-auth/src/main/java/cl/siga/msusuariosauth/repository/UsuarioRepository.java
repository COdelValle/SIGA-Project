package cl.siga.msusuariosauth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import cl.siga.msusuariosauth.model.entity.Usuario;

@Repository 
public interface UsuarioRepository extends JpaRepository<Usuario, String>, JpaSpecificationExecutor<Usuario> {
    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);
}
