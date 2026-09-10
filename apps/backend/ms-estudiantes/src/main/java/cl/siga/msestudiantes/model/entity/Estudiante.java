package cl.siga.msestudiantes.model.entity;

import java.time.LocalDate;
import java.util.List;
import cl.siga.coreshare.validation.RUT;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "estudiantes",
    indexes = {
        @Index (name = "idx_estudiante_rut", columnList = "rut", unique = true)
    })
@Getter 
@Setter 
@Builder 
@AllArgsConstructor
@NoArgsConstructor
public class Estudiante {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long  id;

    @NotBlank (message = "Se requiere ingresar idUsuario")
    @Column(name = "id", length = 36, nullable = false, updatable = false)
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

    @NotBlank(message = "El/los apellido/s no puede/n estar vacio")
    @Size(min = 2, max = 100, message = "El/los apellido/s tiene/n que tener entre 2 a 100 caracteres")
    @Column(nullable = false)
    private String lastName;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    @Column(nullable = false)
    private LocalDate birthDate;

    @ElementCollection
    @Column (name = "allergies", nullable = true)
    private List<String> allergies;

    @NotNull (message = "El estado del estudiante es obligatorio")
    @Enumerated (EnumType.STRING)
    @Column (name = "state", nullable = false, length = 50)
    private State state;
}
