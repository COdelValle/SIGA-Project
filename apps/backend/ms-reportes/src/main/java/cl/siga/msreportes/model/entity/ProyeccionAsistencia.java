package cl.siga.msreportes.model.entity;

import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.coreshare.dto.notificaciones.NivelRiesgo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proyecciones_asistencia",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_proyeccion_asistencia_estudiante_curso",
        columnNames = {"id_estudiante", "id_curso_asignatura"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProyeccionAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "id_estudiante", nullable = false)
    private Long idEstudiante;
    @Column(name = "id_curso_asignatura", nullable = false)
    private Long idCursoAsignatura;
    @Column(name = "id_asistencia", nullable = false)
    private Long idAsistencia;
    @Column(nullable = false)
    private LocalDate fecha;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private State estado;
    @Column(name = "porcentaje_inasistencia", nullable = false)
    private double porcentajeInasistencia;
    @Column(name = "total_registros", nullable = false)
    private long totalRegistros;
    @Column(name = "total_inasistencias", nullable = false)
    private long totalInasistencias;
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_riesgo", nullable = false, length = 20)
    private NivelRiesgo nivelRiesgo;
    @Column(name = "id_evento", nullable = false, length = 36)
    private String idEvento;
    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
