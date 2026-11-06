package cl.siga.ms_kafka.model.entity;

import cl.siga.ms_kafka.dto.events.notificacion.EstadoAsistencia;
import cl.siga.ms_kafka.dto.events.notificacion.NivelRiesgo;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Data
public class AsistenciaProjection {

  private UUID id;

  private Long idEstudiante;
  private Long idCurso;
  private Long idAsignatura;

  private Integer totalClases;
  private Integer totalPresentes;
  private Integer totalAusencias;
  private Integer totalAtrasos;
  private Integer totalJustificados;

  private EstadoAsistencia estado;
  private Double porcentajeAsistencia;
  private Double porcentajeAusencia;

  private NivelRiesgo nivelRiesgo;

  private LocalDateTime ultimaActualizacion;

  public AsistenciaProjection() {

    this.totalClases = 0;
    this.totalPresentes = 0;
    this.totalAusencias = 0;
    this.totalAtrasos = 0;
    this.totalJustificados = 0;

    this.porcentajeAsistencia = 0.0;
    this.porcentajeAusencia = 0.0;

    this.nivelRiesgo = NivelRiesgo.NORMAL;
  }

  public void registrarEstado(EstadoAsistencia estado) {

    totalClases++;

    switch (estado) {

      case PRESENTE -> totalPresentes++;

      case AUSENTE -> totalAusencias++;

      case ATRASO -> totalAtrasos++;

      case JUSTIFICADO -> totalJustificados++;
    }

    recalcularPorcentajes();
  }

  private void recalcularPorcentajes() {

    porcentajeAsistencia =
      (totalPresentes * 100.0) / totalClases;

    porcentajeAusencia =
      (totalAusencias * 100.0) / totalClases;
  }

}
