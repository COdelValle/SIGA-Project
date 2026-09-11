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
    @Column(name = "nombre", unique = true, length = 100, nullable = false)
    private String nombre;

    @NotBlank (message = "La descripción es requerida")
    @Size (min = 2, max = 150)
    @Column(name = "descripcion", length = 255, nullable = true)
    private String descripcion;

    @PrePersist
    @PreUpdate
    private void formatFields() {
        if (nombre != null) {
            nombre = nombre.trim().toUpperCase();
        }
        if (descripcion != null) {
            descripcion = descripcion.trim().toLowerCase();
        }
    }
}