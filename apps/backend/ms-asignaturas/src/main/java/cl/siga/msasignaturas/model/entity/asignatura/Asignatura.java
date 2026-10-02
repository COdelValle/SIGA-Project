package cl.siga.msasignaturas.model.entity.asignatura;

import java.util.List;
import java.util.ArrayList;

import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msasignaturas.model.entity.Horario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "asignaturas")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_asignatura", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@SuperBuilder 
@AllArgsConstructor
@NoArgsConstructor
public abstract class Asignatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 50)
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @NotBlank(message = "La descripción es requerida")
    @Size(min = 2, max = 150)
    @Column(name = "description", length = 150, nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Semestre semestre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AreaAcademica area;

    @Column(name = "id_docente", nullable = false)
    private Long idDocente;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "asignatura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Horario> horarios = new ArrayList<>();

    @PrePersist
    @PreUpdate
    protected void formatFieldsAndValidate() {
        if (name != null) {
            name = name.trim().toUpperCase();
        }
        if (description != null) {
            description = description.trim().toLowerCase();
        }
    }
    
    // Método auxiliar (Helper) para sincronizar la relación bidireccional
    public void addHorario(Horario horario) {
        horarios.add(horario);
        horario.setAsignatura(this);
    }
}
