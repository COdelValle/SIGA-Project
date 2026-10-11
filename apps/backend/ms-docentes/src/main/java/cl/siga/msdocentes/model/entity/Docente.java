package cl.siga.msdocentes.model.entity;

import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.format.NombrePropio;
import cl.siga.coreshare.format.RutNormalizer;
import cl.siga.coreshare.validation.RUT;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "docentes",
  indexes = {
    @Index (name = "idx_docente_rut", columnList = "rut", unique = true)
  })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Docente {
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

  @NotNull(message = "La fecha de contratación es obligatoria")
  @Past(message = "La fecha de contratación debe ser una fecha pasada")
  @Column(nullable = false)
  private LocalDate fechaContratacion;

  @NotNull(message = "El estado activo/inactivo es obligatorio")
  @Column(nullable = false)
  @Builder.Default
  private Boolean activo = true;

  @NotNull(message = "El área académica es obligatoria")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AreaAcademica area;

  @NotEmpty(message = "El docente debe registrar al menos un certificado")
  @Valid
  @OneToMany(mappedBy = "docente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<Certificado> certificados = new ArrayList<>();

  // Métodos helper para gestionar la relación bidireccional
  public void addCertificado(Certificado certificado) {
    certificados.add(certificado);
    certificado.setDocente(this);
  }

  public void removeCertificado(Certificado certificado) {
    certificados.remove(certificado);
    certificado.setDocente(null);
  }

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
