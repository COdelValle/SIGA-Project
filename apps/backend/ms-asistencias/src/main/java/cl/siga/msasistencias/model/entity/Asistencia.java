package cl.siga.msasistencias.model.entity;

import cl.siga.coreshare.dto.asistencia.enums.Justificacion;
import cl.siga.coreshare.dto.asistencia.enums.State;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
  name = "asistencias",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "uk_id_estudiante_id_asignatura_fecha",
      columnNames = {"id_estudiante", "id_asignatura", "fecha"}
    )
  }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Asistencia {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull(message = "El ID del estudiante es obligatorio")
  @Column(name = "id_estudiante", nullable = false)
  private Long idEstudiante;

  @NotNull (message = "El ID del asignatura es obligatorio")
  @Column(name = "id_asignatura", nullable = false)
  private Long idAsignatura;

  @NotNull (message = "La fecha es obligatoria")
  @PastOrPresent (message = "La fecha no puede ser futura")
  @Column(name = "fecha", nullable = false)
  private LocalDate fecha;

  @NotNull (message = "El estado es obligatorio")
  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false)
  private State estado;

  @NotNull (message = "La justificación es obligatoria")
  @Enumerated(EnumType.STRING)
  @Column(name = "justificacion", nullable = false)
  private Justificacion justificacion;

  @Size(max = 255, message = "La observación no puede superar los 255 caracteres")
  @Column(name = "observacion", length = 255)
  private String observacion;
}
