package cl.siga.ms_kafka.service.rules;

import cl.siga.ms_kafka.dto.events.enums.NivelRiesgo;
import org.springframework.stereotype.Component;

@Component
public class AsistenciaRules {

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
