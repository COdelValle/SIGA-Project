package cl.siga.msasignaturas.model.entity.asignatura;

import cl.siga.coreshare.enums.AreaAcademica;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Asignatura del catálogo general. No pertenece a un curso ni tiene docente,
 * semestre u horarios propios: eso vive en {@link CursoAsignatura}.
 */
@Entity
@Table(name = "asignaturas")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Asignatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 80)
    @Column(name = "nombre", length = 80, nullable = false)
    private String nombre;

    @Size(max = 40)
    @Column(name = "nombre_corto", length = 40)
    private String nombreCorto;

    @NotBlank(message = "La descripción es requerida")
    @Size(min = 2, max = 150)
    @Column(name = "descripcion", length = 150, nullable = false)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "area", nullable = false)
    private AreaAcademica area;

    @Builder.Default
    @Column(name = "calificable", nullable = false)
    private boolean calificable = true;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @PrePersist
    @PreUpdate
    protected void normalizar() {
        if (nombre != null) {
            nombre = nombre.trim();
        }
        if (nombreCorto != null) {
            nombreCorto = nombreCorto.trim();
        }
        if (descripcion != null) {
            descripcion = descripcion.trim();
        }
    }
}
