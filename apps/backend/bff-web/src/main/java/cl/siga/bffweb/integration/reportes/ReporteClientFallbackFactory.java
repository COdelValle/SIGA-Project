package cl.siga.bffweb.integration.reportes;

import cl.siga.bffweb.integration.FeignErrorTranslator;
import cl.siga.coreshare.dto.reportes.IndicadorAsistenciaDTO;
import cl.siga.coreshare.dto.reportes.IndicadorRendimientoDTO;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReporteClientFallbackFactory implements FallbackFactory<ReporteClient> {

    private final FeignErrorTranslator translator;

    @Override
    public ReporteClient create(Throwable cause) {
        return new ReporteClient() {
            @Override
            public List<IndicadorAsistenciaDTO> buscarAsistencia(
                    Long estudiante, Long curso, LocalDate from, LocalDate to) {
                throw translator.traducir(cause, "No se pudieron obtener los indicadores de asistencia.");
            }

            @Override
            public List<IndicadorRendimientoDTO> buscarRendimiento(
                    Long estudiante, Long curso, LocalDate from, LocalDate to) {
                throw translator.traducir(cause, "No se pudieron obtener los indicadores de rendimiento.");
            }
        };
    }
}
