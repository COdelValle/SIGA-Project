package cl.siga.msasignaturas.model.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.msasignaturas.model.entity.asignatura.AsignaturaElectiva;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inscripciones", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"id_alumno", "asignatura_id"})
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_alumno", nullable = false)
    private Long idAlumno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private AsignaturaElectiva asignatura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInscripcion estado;

    @CreationTimestamp
    @Column(name = "fecha_inscripcion", updatable = false)
    private LocalDateTime fechaInscripcion;
}