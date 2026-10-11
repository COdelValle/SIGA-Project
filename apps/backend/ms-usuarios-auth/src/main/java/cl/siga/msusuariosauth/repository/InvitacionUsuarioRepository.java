package cl.siga.msusuariosauth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.siga.msusuariosauth.model.entity.InvitacionUsuario;

@Repository
public interface InvitacionUsuarioRepository extends JpaRepository<InvitacionUsuario, String> {
    Optional<InvitacionUsuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
