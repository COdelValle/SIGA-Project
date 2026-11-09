package cl.siga.ms_kafka.repository;

import cl.siga.ms_kafka.model.entity.AsistenciaProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface kafkaRepository
  extends JpaRepository<
    AsistenciaProjection,
    UUID > {

  Optional<AsistenciaProjection>
  findByIdEstudiante(Long idEstudiante);
}
