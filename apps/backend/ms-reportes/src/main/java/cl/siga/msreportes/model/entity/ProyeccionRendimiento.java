package cl.siga.msreportes.model.entity;

import cl.siga.coreshare.dto.notificaciones.NivelRiesgo;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proyecciones_rendimiento",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_proyeccion_rendimiento_estudiante_evaluacion",
        columnNames = {"id_estudiante", "id_evaluacion"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProyeccionRendimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "id_estudiante", nullable = false)
    private Long idEstudiante;
    @Column(name = "id_evaluacion", nullable = false)
    private Long idEvaluacion;
    @Column(name = "id_curso_asignatura")
    private Long idCursoAsignatura;
    @Column(name = "id_nota", nullable = false)
    private Long idNota;
    private Double puntaje;
    private Double promedio;
    @Column(name = "evaluaciones_insuficientes", nullable = false)
    private long evaluacionesInsuficientes;
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
