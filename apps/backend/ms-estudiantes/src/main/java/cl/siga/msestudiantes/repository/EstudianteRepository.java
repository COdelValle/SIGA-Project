package cl.siga.msestudiantes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.siga.msestudiantes.model.entity.Estudiante;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    // Aquí puedes agregar métodos de consulta personalizados si es necesario
}
