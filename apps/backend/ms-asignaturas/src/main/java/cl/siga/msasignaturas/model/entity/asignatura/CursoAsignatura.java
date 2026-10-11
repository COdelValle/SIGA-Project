package cl.siga.msasignaturas.model.entity.asignatura;

import java.util.ArrayList;
import java.util.List;

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.msasignaturas.model.entity.Horario;
import cl.siga.msasignaturas.model.entity.Inscripcion;
import jakarta.persistence.*;
import lombok.*;

/**
 * Dictación concreta de una asignatura del catálogo en un curso. Aquí viven el
 * docente, el semestre, los horarios y los cupos; los datos curriculares
 * (nombre, área, calificable) se resuelven desde {@link Asignatura}.
 */
@Entity
@Table(name = "cursos_asignaturas", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"id_asignatura", "id_clase", "semestre"})
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CursoAsignatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_asignatura", nullable = false)
    private Asignatura asignatura;

    @Column(name = "id_clase", nullable = false)
    private Long idClase;

    @Column(name = "id_docente", nullable = false)
    private Long idDocente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Semestre semestre;

    @Enumerated(EnumType.STRING)
    @Column(name = "caracter", nullable = false, length = 20)
    private CaracterAsignatura caracter;

    @Column(name = "cupo_maximo")
    private Integer cupoMaximo;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "cursoAsignatura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Horario> horarios = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "cursoAsignatura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inscripcion> inscripciones = new ArrayList<>();

    public void addHorario(Horario horario) {
        horarios.add(horario);
        horario.setCursoAsignatura(this);
    }
}
