package cl.siga.msreportes.service;

import cl.siga.coreshare.dto.reportes.IndicadorAsistenciaDTO;
import cl.siga.coreshare.dto.reportes.IndicadorRendimientoDTO;
import cl.siga.msreportes.model.entity.ProyeccionAsistencia;
import cl.siga.msreportes.model.entity.ProyeccionRendimiento;
import cl.siga.msreportes.repository.ProyeccionAsistenciaRepository;
import cl.siga.msreportes.repository.ProyeccionRendimientoRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReporteConsultaService {

    private final ProyeccionAsistenciaRepository asistenciaRepository;
    private final ProyeccionRendimientoRepository rendimientoRepository;

    public List<IndicadorAsistenciaDTO> buscarAsistencia(
            Long idEstudiante, Long idCursoAsignatura, LocalDate from, LocalDate to) {
        validarPeriodo(from, to);
        return asistenciaRepository.buscar(idEstudiante, idCursoAsignatura, from, to).stream()
            .map(ReporteConsultaService::toDto)
            .toList();
    }

    public List<IndicadorRendimientoDTO> buscarRendimiento(
            Long idEstudiante, Long idCursoAsignatura, LocalDate from, LocalDate to) {
        validarPeriodo(from, to);
        return rendimientoRepository.buscar(idEstudiante, idCursoAsignatura,
            from == null ? null : from.atStartOfDay(),
            to == null ? null : to.plusDays(1).atStartOfDay()).stream()
            .map(ReporteConsultaService::toDto)
            .toList();
    }

    private static void validarPeriodo(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
    }

    private static IndicadorAsistenciaDTO toDto(ProyeccionAsistencia p) {
        return new IndicadorAsistenciaDTO(p.getIdEstudiante(), p.getIdCursoAsignatura(), p.getFecha(),
            p.getEstado(), p.getPorcentajeInasistencia(), p.getTotalRegistros(), p.getTotalInasistencias(),
            p.getNivelRiesgo(), p.getActualizadoEn());
    }

    private static IndicadorRendimientoDTO toDto(ProyeccionRendimiento p) {
        return new IndicadorRendimientoDTO(p.getIdEstudiante(), p.getIdEvaluacion(), p.getIdCursoAsignatura(),
            p.getPuntaje(), p.getPromedio(), p.getEvaluacionesInsuficientes(), p.getNivelRiesgo(),
            p.getActualizadoEn());
    }
}
