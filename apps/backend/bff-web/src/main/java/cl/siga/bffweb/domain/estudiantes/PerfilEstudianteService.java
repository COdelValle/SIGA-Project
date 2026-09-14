package cl.siga.bffweb.domain.estudiantes;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.siga.bffweb.domain.estudiantes.dto.api.PerfilEstudianteResponseDTO;
import cl.siga.bffweb.domain.estudiantes.mapper.PerfilEstudianteMapper;
import cl.siga.bffweb.integration.Notas.NotaClient;
import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class PerfilEstudianteService {
    private final EstudianteClient estudianteClient;
    private final AsignaturaClient asignaturaClient;
    private final NotaClient notaClient;

    private final PerfilEstudianteMapper mapper;

    @Transactional (readOnly = true)
    public PerfilEstudianteResponseDTO getPerfil(String idExterno) {
        
        // 1. Consumes el API de Estudiantes usando el ID público (String)
        EstudianteResponseDTO estudiante = estudianteClient.getEstudianteById(idExterno);

        // 2. Extraes el ID interno (Long) para resolver las relaciones
        Long idInternoEstudiante = estudiante.id(); 
        
        // 3. Buscas las notas usando el ID interno
        List<NotaResponseDTO> notas = notaClient.getNotaByIdEstudiante(idInternoEstudiante);

        // 4. Extraes los IDs internos de las asignaturas y las vas a buscar
        List<AsignaturaResponseDTO> asignaturas = notas.stream()
            .map(NotaResponseDTO::idAsignatura) // Sacas el ID interno de la asignatura
            .distinct()
            .map(asignaturaClient::getAsignaturaById)
            .toList();

        // 5. El Mapper une todo y genera la estructura anidada para el frontend
        return mapper.toResponse(estudiante, asignaturas, notas);
    }
}
