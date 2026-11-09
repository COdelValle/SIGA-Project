package cl.siga.msreportes.model.entity;

import cl.siga.coreshare.dto.notificaciones.NivelRiesgo;
import cl.siga.coreshare.dto.notificaciones.TipoRiesgo;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

/**
 * Alerta de negocio lista para ser publicada por el adaptador RabbitMQ.
 * La clave de idempotencia impide generar dos veces la misma alerta.
 */
@Entity
@Table(name = "alertas_riesgo",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_alerta_riesgo_idempotencia",
        columnNames = "clave_idempotencia"))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertaRiesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clave_idempotencia", nullable = false, unique = true, length = 180)
    private String claveIdempotencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_riesgo", nullable = false, length = 32)
    private TipoRiesgo tipoRiesgo;

    @Column(name = "id_estudiante", nullable = false)
    private Long idEstudiante;

    @Column(name = "id_curso_asignatura")
    private Long idCursoAsignatura;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_anterior", length = 20)
    private NivelRiesgo nivelAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_nuevo", nullable = false, length = 20)
    private NivelRiesgo nivelNuevo;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(name = "id_evento", nullable = false, length = 36)
    private String idEvento;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;
}
