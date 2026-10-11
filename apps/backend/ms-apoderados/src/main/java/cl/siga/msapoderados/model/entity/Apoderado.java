package cl.siga.msapoderados.model.entity;

import cl.siga.coreshare.format.NombrePropio;
import cl.siga.coreshare.format.RutNormalizer;
import cl.siga.coreshare.validation.Phone;
import cl.siga.coreshare.validation.RUT;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "apoderados",
  indexes = {
    @Index(name = "idx_apoderado_rut", columnList = "rut", unique = true)
  })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Apoderado {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long  id;

  @NotBlank(message = "Se requiere ingresar idUsuario")
  @Size(min = 36, max = 36, message = "El ID de Azure debe tener exactamente 36 caracteres")
  @Column(name = "id_usuario", length = 36, nullable = false, updatable = false)
  private String idUsuario;

  @NotBlank (message = "Se requiere ingresar RUT")
  @RUT(message = "RUT invalido")
  @Column(unique = true, nullable = false)
  private String rut;

  @NotBlank(message = "El nombre no puede estar vacio")
  @Size(min = 2, max = 50, message = "El nombre tiene que tener entre 2 a 50 caracteres")
  @Column(nullable = false)
  private String firstName;

  @Size(max = 50, message = "El segundo nombre puede tener máximo 50 caracteres")
  @Column(nullable = true)
  private String middleName;

  @NotBlank(message = "El primer apellido no puede/n estar vacio")
  @Size(min = 2, max = 50, message = "El/los apellido/s tiene/n que tener entre 2 a 50 caracteres")
  @Column(nullable = false)
  private String firstSurname;

  @Size(max = 50, message = "El segundo apellido puede tener máximo 50 caracteres")
  @Column(nullable = true)
  private String secondSurname;

  @ElementCollection
  @CollectionTable(
    name = "apoderado_telefonos",
    joinColumns = @JoinColumn(name = "apoderado_id")
  )
  @Column(name = "telefono", nullable = false)
  @NotEmpty(message = "Se requiere al menos un teléfono")
  @Builder.Default
  private List<@Valid @Phone String> telefonos = new ArrayList<>();

  @ElementCollection
  @CollectionTable(
    name = "apoderado_estudiantes",
    joinColumns = @JoinColumn(name = "apoderado_id")
  )
  @Valid
  @NotEmpty(message = "Se requiere al menos un estudiante")
  @Builder.Default
  private List<ApoderadoEstudiante> estudiantes = new ArrayList<>();

  @NotNull(message = "El estado activo/inactivo es obligatorio")
  @Column(nullable = false)
  @Builder.Default
  private Boolean activo = true;

  @PrePersist
  @PreUpdate
  public void prePersist() {
    this.rut = RutNormalizer.normalizar(this.rut);
    this.firstName = NombrePropio.normalizar(this.firstName);
    this.middleName = NombrePropio.normalizar(this.middleName);
    this.firstSurname = NombrePropio.normalizar(this.firstSurname);
    this.secondSurname = NombrePropio.normalizar(this.secondSurname);
  }
}
