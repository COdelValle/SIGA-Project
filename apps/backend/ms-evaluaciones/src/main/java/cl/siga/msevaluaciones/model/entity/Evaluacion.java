package cl.siga.msevaluaciones.model.entity;

import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "evaluaciones")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Evaluacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "El nombre es requerido")
  @Size(min = 2, max = 50)
  @Column(name = "nombre", nullable = false, length = 50)
  private String nombre;

  @NotNull(message = "El tipo es requerido")
  @Enumerated(EnumType.STRING)
  @Column(name = "tipo", nullable = false)
  private TipoEvaluacion tipo;

  @NotNull(message = "La ponderación es requerida")
  @DecimalMin(value = "0.0", message = "La ponderación debe ser mayor o igual a 0")
  @DecimalMax(value = "100.0", message = "La ponderación debe ser menor o igual a 100")
  @Column(name = "ponderacion", nullable = false)
  private Double ponderacion;

  @NotNull(message = "El ID de la asignatura es requerido")
  @Column(name = "id_asignatura", nullable = false)
  private Long idAsignatura;

  @Builder.Default
  @NotNull(message = "El estado activo es obligatorio")
  @Column(name = "active", nullable = false)
  private boolean active = true;

  @PrePersist
  @PreUpdate
  public void prePersistAndUpdate() {
    if (this.nombre != null) {
      this.nombre = this.nombre.toUpperCase().trim();
    }
  }
}
