package cl.siga.msapoderados.model.entity;

import cl.siga.coreshare.dto.apoderado.parentesco.enums.Parentesco;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApoderadoEstudiante {
  @Column(name = "estudiante_id", nullable = false)
  @NotNull(message = "Se requiere ingresar idEstudiante")
  private Long idEstudiante;

  @NotNull(message = "Se requiere ingresar parentesco")
  @Enumerated(EnumType.STRING)
  @Column(name = "parentesco", nullable = false)
  private Parentesco parentesco;
}
