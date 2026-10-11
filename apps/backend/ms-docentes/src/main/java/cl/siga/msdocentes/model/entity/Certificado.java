package cl.siga.msdocentes.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "certificados")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Certificado {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "El nombre del certificado no puede estar vacío")
  @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
  @Column(nullable = false, length = 100)
  private String nombre;

  @NotBlank(message = "La institución no puede estar vacía")
  @Size(max = 150, message = "La institución no puede superar los 150 caracteres")
  @Column(name = "institucion_realizacion", nullable = false, length = 150)
  private String institucionRealizacion;

  @NotNull(message = "La fecha de titulación es obligatoria")
  @PastOrPresent(message = "La fecha de titulación debe ser una fecha pasada o actual")
  @Column(name = "fecha_titulacion", nullable = false)
  private LocalDate fechaTitulacion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_docente", nullable = false)
  private Docente docente;
}
