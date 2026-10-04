package cl.siga.bffweb.domain.evaluaciones;

import org.springframework.stereotype.Service;

import cl.siga.bffweb.domain.docentes.DocenteContextService;
import cl.siga.bffweb.integration.evaluaciones.EvaluacionClient;
import cl.siga.coreshare.dto.evaluaciones.ActualizarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.RegistrarEvaluacionRequestDTO;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Escrituras de evaluaciones para el portal docente. La regla de ponderacion
 * acumulada <= 100 la garantiza ms-evaluaciones; aqui solo se valida que la
 * asignatura pertenezca al docente autenticado.
 */
@Service
@RequiredArgsConstructor
public class EvaluacionBffService {
    private final EvaluacionClient evaluacionClient;
    private final DocenteContextService docenteContext;

    public EvaluacionResponseDTO crearEvaluacion(RegistrarEvaluacionRequestDTO request) {
        docenteContext.validarAsignaturaDelDocente(request.idAsignatura());
        return evaluacionClient.saveEvaluacion(request);
    }

    public EvaluacionResponseDTO editarEvaluacion(Long id, ActualizarEvaluacionRequestDTO request) {
        EvaluacionResponseDTO evaluacion = evaluacionDe(id);
        docenteContext.validarAsignaturaDelDocente(evaluacion.idAsignatura());
        return evaluacionClient.updateEvaluacion(id, request);
    }

    public void eliminarEvaluacion(Long id) {
        EvaluacionResponseDTO evaluacion = evaluacionDe(id);
        docenteContext.validarAsignaturaDelDocente(evaluacion.idAsignatura());
        evaluacionClient.deleteEvaluacion(id);
    }

    private EvaluacionResponseDTO evaluacionDe(Long id) {
        EvaluacionResponseDTO evaluacion = evaluacionClient.getEvaluacionById(id);
        if (evaluacion == null) {
            throw new ResourceNotFoundException("Evaluación con ID " + id + " no encontrada.");
        }
        return evaluacion;
    }
}
