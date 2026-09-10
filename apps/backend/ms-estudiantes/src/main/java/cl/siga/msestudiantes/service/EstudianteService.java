package cl.siga.msestudiantes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.siga.coreshare.dto.estudiante.ActualizarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.estudiante.RegistrarEstudianteRequestDTO;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msestudiantes.model.entity.Estudiante;
import cl.siga.msestudiantes.model.mapper.EstudianteMapper;
import cl.siga.msestudiantes.repository.EstudianteRepository;

@Service
public class EstudianteService {
    @Autowired
    private EstudianteRepository repository;

    @Autowired 
    private EstudianteMapper mapper;

    public EstudianteResponseDTO guardarEstudiante(RegistrarEstudianteRequestDTO request) {
        // Convertimos el Record Request en Entidad JPA
        Estudiante estudiante = mapper.toEntity(request);

        Estudiante guardado = repository.save(estudiante);

        // Convertimos la Entidad guardada en el Record Response
        return mapper.toResponseDto(guardado);
    }

    public EstudianteResponseDTO actualizar(Long id, ActualizarEstudianteRequestDTO request) {
        // 1. Buscas la entidad actual en la BD
        Estudiante estudianteExistente = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Estudiante con ID " + id + " no encontrado."));

        // 2. MapStruct sobreescribe firstName, lastName, etc., pero el RUT queda INTACTO
        mapper.updateEntityFromDto(request, estudianteExistente);

        // 3. Guardas los cambios
        return mapper.toResponseDto(repository.save(estudianteExistente));
    }
}
