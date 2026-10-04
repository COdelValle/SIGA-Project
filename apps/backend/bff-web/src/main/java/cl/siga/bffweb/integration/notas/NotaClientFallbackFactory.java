package cl.siga.bffweb.integration.notas;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import cl.siga.bffweb.integration.FeignErrorTranslator;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.notas.ActualizarNotaRequestDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.dto.notas.RegistrarNotaRequestDTO;
import lombok.RequiredArgsConstructor;

/**
 * Fallback de notas que conserva los errores reales del microservicio
 * (400 validacion, 404, 409 duplicado) y usa 503 solo ante fallos de conexion.
 */
@Component
@RequiredArgsConstructor
public class NotaClientFallbackFactory implements FallbackFactory<NotaClient> {
    private final FeignErrorTranslator translator;

    @Override
    public NotaClient create(Throwable cause) {
        return new NotaClient() {
            @Override
            public NotaResponseDTO getNotaById(Long id) {
                throw translator.traducir(cause, "No se pudo obtener la nota " + id);
            }

            @Override
            public PageResponseDTO<NotaResponseDTO> searchNotas(Long idEstudiante, int size) {
                throw translator.traducir(cause,
                    "No se pudieron obtener las notas del estudiante " + idEstudiante);
            }

            @Override
            public PageResponseDTO<NotaResponseDTO> searchNotasByEvaluacion(Long idEvaluacion, int size) {
                throw translator.traducir(cause,
                    "No se pudieron obtener las notas de la evaluacion " + idEvaluacion);
            }

            @Override
            public NotaResponseDTO saveNota(RegistrarNotaRequestDTO request) {
                throw translator.traducir(cause, "No se pudo registrar la nota");
            }

            @Override
            public NotaResponseDTO updateNota(Long id, ActualizarNotaRequestDTO request) {
                throw translator.traducir(cause, "No se pudo actualizar la nota " + id);
            }

            @Override
            public void deleteNota(Long id) {
                throw translator.traducir(cause, "No se pudo eliminar la nota " + id);
            }
        };
    }
}
