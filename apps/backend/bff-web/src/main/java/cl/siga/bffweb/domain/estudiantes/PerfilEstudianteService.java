package cl.siga.bffweb.domain.estudiantes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.siga.bffweb.domain.estudiantes.dto.api.PerfilEstudianteResponseDTO;
import cl.siga.bffweb.domain.estudiantes.mapper.PerfilEstudianteMapper;
import cl.siga.bffweb.integration.Notas.NotaClient;
import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.bffweb.integration.evaluaciones.EvaluacionClient;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class PerfilEstudianteService {
    private final EstudianteClient estudianteClient;
    private final AsignaturaClient asignaturaClient;
    private final NotaClient notaClient;
    private final EvaluacionClient evaluacionClient;

    private final PerfilEstudianteMapper mapper;

    @Transactional (readOnly = true)
    public PerfilEstudianteResponseDTO getPerfil(String idExterno) {
        
        // 1. Consumes el API de Estudiantes usando el ID público (String)
        EstudianteResponseDTO estudiante = estudianteClient.getEstudianteById(idExterno);

        // 2. Extraes el ID interno (Long) para resolver las relaciones
        Long idInternoEstudiante = estudiante.id(); 

        // 3. Buscas las notas usando el ID interno
        List<NotaResponseDTO> notas = notaClient.getNotaByIdEstudiante(idInternoEstudiante);

        // 4. Las notas referencian evaluaciones: resolvemos evaluación -> asignatura
        Map<Long, Long> evaluacionAAsignatura = notas.stream()
            .map(NotaResponseDTO::idEvaluacion)
            .distinct()
            .map(evaluacionClient::getEvaluacionById)
            .collect(Collectors.toMap(EvaluacionResponseDTO::id, EvaluacionResponseDTO::idAsignatura));

        // 5. Buscas las asignaturas referenciadas por esas evaluaciones
        List<AsignaturaResponseDTO> asignaturas = evaluacionAAsignatura.values().stream()
            .distinct()
            .map(asignaturaClient::getAsignaturaById)
            .toList();

        // 6. El Mapper une todo y genera la estructura anidada para el frontend
        return mapper.toResponse(estudiante, asignaturas, notas, evaluacionAAsignatura);
    }
}
