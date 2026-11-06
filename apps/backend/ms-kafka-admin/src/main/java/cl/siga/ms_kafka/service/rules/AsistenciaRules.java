package cl.siga.ms_kafka.service.rules;

import cl.siga.ms_kafka.dto.events.notificacion.EstadoAsistencia;
import cl.siga.ms_kafka.dto.events.notificacion.NivelRiesgo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static cl.siga.ms_kafka.dto.events.notificacion.EstadoAsistencia.*;

@Slf4j
@Component
public class AsistenciaRules {

  public EstadoAsistencia obtenerEstado(
    EstadoAsistencia projection) {

    return switch (projection) {

      case AUSENTE -> AUSENTE;

      case ATRASO -> ATRASO;

      case JUSTIFICADO -> JUSTIFICADO;

      case PRESENTE -> PRESENTE;
    };
  }

  public NivelRiesgo calcularRiesgo(
    double porcentajeAusencia) {

    if (porcentajeAusencia >= 60) {
      return NivelRiesgo.CRITICO;
    }

    if (porcentajeAusencia >= 40) {
      return NivelRiesgo.ALTO;
    }

    if (porcentajeAusencia >= 20) {
      return NivelRiesgo.PREVENTIVO;
    }

    return NivelRiesgo.NORMAL;
  }
}
