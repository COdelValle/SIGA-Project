package cl.siga.msasignaturas.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity 
@Table (name = "asignaturas")
@Getter 
@Setter 
@Builder 
@AllArgsConstructor
@NoArgsConstructor
public class Asignatura {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long  id;

    @NotBlank (message = "El nombre es requerido")
    @Size (min = 2, max = 50)
    @Column(name = "name", unique = true, length = 50, nullable = false)
    private String name;

    @NotBlank (message = "La descripción es requerida")
    @Size (min = 2, max = 150)
    @Column(name = "description", length = 150, nullable = false)
    private String description;

    @PrePersist
    @PreUpdate
    private void formatFields() {
        if (name != null) {
            name = name.trim().toUpperCase();
        }
        if (description != null) {
            description = description.trim().toLowerCase();
        }
    }
}
