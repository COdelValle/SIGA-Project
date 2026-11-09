package cl.siga.msreportes.model.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "eventos_reportes_procesados")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoReporteProcesado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_evento", nullable = false, unique = true, length = 36)
    private String idEvento;

    @Column(nullable = false, length = 160)
    private String topic;

    private Integer particion;

    @Column(name = "offset_kafka")
    private Long offsetKafka;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "timestamp_evento", nullable = false)
    private LocalDateTime timestampEvento;

    @Column(name = "procesado_en", nullable = false)
    private LocalDateTime procesadoEn;
}
