package cl.siga.bffweb.integration.evaluaciones;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import cl.siga.bffweb.integration.FeignErrorTranslator;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.ActualizarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.RegistrarEvaluacionRequestDTO;
import lombok.RequiredArgsConstructor;

/**
 * Fallback de evaluaciones que conserva los errores reales del microservicio
 * (400 ponderacion, 404, 409) y usa 503 solo ante fallos de conexion.
 */
@Component
@RequiredArgsConstructor
public class EvaluacionClientFallbackFactory implements FallbackFactory<EvaluacionClient> {
    private final FeignErrorTranslator translator;

    @Override
    public EvaluacionClient create(Throwable cause) {
        return new EvaluacionClient() {
            @Override
            public EvaluacionResponseDTO getEvaluacionById(Long id) {
                throw translator.traducir(cause, "No se pudo obtener la evaluacion " + id);
            }

            @Override
            public PageResponseDTO<EvaluacionResponseDTO> searchEvaluacionesByAsignatura(
                    Long idAsignatura, int size) {
                throw translator.traducir(cause,
                    "No se pudieron obtener las evaluaciones de la asignatura " + idAsignatura);
            }

            @Override
            public EvaluacionResponseDTO saveEvaluacion(RegistrarEvaluacionRequestDTO request) {
                throw translator.traducir(cause, "No se pudo registrar la evaluacion");
            }

            @Override
            public EvaluacionResponseDTO updateEvaluacion(Long id, ActualizarEvaluacionRequestDTO request) {
                throw translator.traducir(cause, "No se pudo actualizar la evaluacion " + id);
            }

            @Override
            public void deleteEvaluacion(Long id) {
                throw translator.traducir(cause, "No se pudo eliminar la evaluacion " + id);
            }
        };
    }
}
