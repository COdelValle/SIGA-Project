package cl.siga.msusuariosauth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.siga.msusuariosauth.model.entity.UserRegistrationProcess;

public interface UserRegistrationProcessRepository extends JpaRepository<UserRegistrationProcess, String> {
}
