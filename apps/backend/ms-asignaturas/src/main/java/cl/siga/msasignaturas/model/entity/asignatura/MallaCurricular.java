package cl.siga.msasignaturas.model.entity.asignatura;

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.PlanFormacion;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import jakarta.persistence.*;
import lombok.*;

/**
 * Fila de la malla curricular: qué asignatura del catálogo aplica a un nivel,
 * con qué carácter y en qué plan. Es la plantilla que valida/genera las
 * dictaciones de cada curso.
 */
@Entity
@Table(name = "malla_curricular", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"nivel", "id_asignatura", "plan"})
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MallaCurricular {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false)
    private Nivel nivel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_asignatura", nullable = false)
    private Asignatura asignatura;

    @Enumerated(EnumType.STRING)
    @Column(name = "caracter", nullable = false, length = 20)
    private CaracterAsignatura caracter;

    @Column(name = "horas_semanales")
    private Integer horasSemanales;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 30)
    private PlanFormacion plan;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
