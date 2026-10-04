package cl.siga.bffweb.integration.asistencias;

import java.time.LocalDate;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import cl.siga.bffweb.integration.FeignErrorTranslator;
import cl.siga.coreshare.dto.asistencia.ActualizarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.asistencia.RegistrarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import lombok.RequiredArgsConstructor;

/**
 * Fallback de asistencias que conserva los errores reales del microservicio
 * (400 validacion, 404, 409 duplicado) y usa 503 solo ante fallos de conexion.
 */
@Component
@RequiredArgsConstructor
public class AsistenciaClientFallbackFactory implements FallbackFactory<AsistenciaClient> {
    private final FeignErrorTranslator translator;

    @Override
    public AsistenciaClient create(Throwable cause) {
        return new AsistenciaClient() {
            @Override
            public PageResponseDTO<AsistenciaResponseDTO> searchAsistencias(Long idEstudiante, int size) {
                throw translator.traducir(cause,
                    "No se pudieron obtener las asistencias del estudiante " + idEstudiante);
            }

            @Override
            public PageResponseDTO<AsistenciaResponseDTO> searchAsistenciasByAsignatura(
                    Long idAsignatura, LocalDate from, LocalDate to, int size) {
                throw translator.traducir(cause,
                    "No se pudieron obtener las asistencias de la asignatura " + idAsignatura);
            }

            @Override
            public AsistenciaResponseDTO getAsistenciaById(Long id) {
                throw translator.traducir(cause, "No se pudo obtener la asistencia " + id);
            }

            @Override
            public AsistenciaResponseDTO saveAsistencia(RegistrarAsistenciaRequestDTO request) {
                throw translator.traducir(cause, "No se pudo registrar la asistencia");
            }

            @Override
            public AsistenciaResponseDTO updateAsistencia(Long id, ActualizarAsistenciaRequestDTO request) {
                throw translator.traducir(cause, "No se pudo actualizar la asistencia " + id);
            }
        };
    }
}
