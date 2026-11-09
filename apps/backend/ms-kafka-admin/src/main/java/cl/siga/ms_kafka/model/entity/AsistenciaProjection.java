package cl.siga.ms_kafka.model.entity;

import cl.siga.ms_kafka.dto.events.notificacion.EstadoAsistencia;
import cl.siga.ms_kafka.dto.events.notificacion.NivelRiesgo;
import cl.siga.coreshare.dto.asistencia.enums.State;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@Data
@Entity
@Table(name = "asistencia_projection")
public class AsistenciaProjection {

  @Id
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

  @ElementCollection
  @CollectionTable(
    name = "asistencia_contadores",
    joinColumns = @JoinColumn(name = "projection_id")
  )
  @MapKeyEnumerated(EnumType.STRING)
  @Column(name = "cantidad")
  private Map<State, Integer> estados =
    new EnumMap<>(State.class);
  public AsistenciaProjection() {

    for (State estado : State.values()) {
      estados.put(estado, 0);
    };

    this.totalClases = 0;
    this.totalPresentes = 0;
    this.totalAusencias = 0;
    this.totalAtrasos = 0;
    this.totalJustificados = 0;

    this.porcentajeAsistencia = 0.0;
    this.porcentajeAusencia = 0.0;

    this.nivelRiesgo = NivelRiesgo.NORMAL;

  }

  public void calcularEstado(State estado) {

    estados.computeIfPresent(
      estado,
      (k, v) -> v + 1
    );
  }

  public void regEstado(State estado) {

    estados.put(
      estado,
      estados.get(estado) + 1
    );
  }

  private Integer totalClases() {

    int presentes =
      estados.getOrDefault(
        State.PRESENTE,
        0
      );

    int ausencias =
      estados.getOrDefault(
        State.AUSENTE,
        0
      );

    return presentes + ausencias;
  }

  public int totalAusencias(){
    return estados.getOrDefault(
      State.AUSENTE,
      0
    );
  }

  public double porcentajeAusencias() {

    int total = totalClases();

    if (total == 0) {
      return 0.0;
    }

    return (totalAusencias() * 100.0)
      / total;
  }

  public NivelRiesgo calcularNivelRiesgo() {

    double percent = porcentajeAusencias();

    if (percent >= 60) {
      return NivelRiesgo.CRITICO;
    }

    if (percent  >= 40) {
      return NivelRiesgo.ALTO;
    }

    if (percent  >= 20) {
      return NivelRiesgo.PREVENTIVO;
    }

    return NivelRiesgo.NORMAL;
  }

  public void actualizarIndicadores() {

    this.porcentajeAusencia =
      porcentajeAusencias();

    this.porcentajeAsistencia =
      100 - porcentajeAusencia;

    this.nivelRiesgo =
      calcularNivelRiesgo();
  }
}
