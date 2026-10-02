package cl.siga.msasignaturas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import cl.siga.coreshare.dto.asignatura.horario.enums.DiaSemana;
import cl.siga.msasignaturas.model.entity.Horario;

@Repository 
public interface HorarioRepository extends JpaRepository<Horario, Long>, JpaSpecificationExecutor<Horario> {

    List<Horario> findByAsignaturaId(Long asignaturaId);

    List<Horario> findByUbicacionIgnoreCaseAndDia(String ubicacion, DiaSemana dia);

    long countByAsignaturaId(Long asignaturaId);
}