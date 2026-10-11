package cl.siga.msusuariosauth.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.siga.coreshare.dto.usuario.enums.RegistrationProcessState;
import cl.siga.msusuariosauth.model.entity.UserRegistrationProcess;

public interface UserRegistrationProcessRepository extends JpaRepository<UserRegistrationProcess, String> {

    boolean existsByEmailAndStateIn(String email, Collection<RegistrationProcessState> states);
}
