package cl.siga.msasignaturas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import cl.siga.msasignaturas.model.entity.Horario;

@Repository 
public interface HorarioRepository extends JpaRepository<Horario, Long>, JpaSpecificationExecutor<Horario> {
}