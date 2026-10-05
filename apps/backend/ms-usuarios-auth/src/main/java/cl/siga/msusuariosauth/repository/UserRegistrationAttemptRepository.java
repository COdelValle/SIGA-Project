package cl.siga.msusuariosauth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.siga.msusuariosauth.model.entity.UserRegistrationAttempt;

public interface UserRegistrationAttemptRepository extends JpaRepository<UserRegistrationAttempt, Long> {
}
