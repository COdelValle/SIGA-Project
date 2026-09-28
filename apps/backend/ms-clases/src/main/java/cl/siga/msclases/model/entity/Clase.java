package cl.siga.msclases.model.entity;

import cl.siga.coreshare.dto.clase.enums.Nivel;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(
  name = "clases",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "uk_nivel_letra_anio",
      columnNames = {"nivel", "letra", "anio_academico"}
    )
  }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Clase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull(message = "El nivel es obligatorio")
  @Enumerated(EnumType.STRING)
  @Column(name = "nivel", nullable = false)
  private Nivel nivel;

  @NotBlank (message = "La letra es obligatoria")
  @Pattern(regexp = "^[A-ZÑ]$", message = "La letra debe ser un único carácter alfabético (A-Z)")
  @Column(name = "letra", nullable = false, length = 1)
  private String letra;

  @NotNull(message = "El año académico es obligatorio")
  @Min(value = 2000, message = "El año académico debe ser igual o mayor a 2000")
  @Column(name = "anio_academico", nullable = false)
  private Integer anioAcademico;

  @Column(name = "id_docente_jefe")
  private Long idDocenteJefe;

  @Builder.Default
  @NotNull(message = "El estado activo es obligatorio")
  @Column(name = "active", nullable = false)
  private boolean active = true;

  @PrePersist
  @PreUpdate
  public void prePersistAndUpdate() {
    if (this.letra != null) {
      this.letra = this.letra.trim().toUpperCase();
    }
  }
}
