package cl.siga.msnotificaciones.model.entity;

import cl.siga.coreshare.dto.notificaciones.TipoDestinoNotificacion;
import cl.siga.coreshare.dto.notificaciones.TipoNotificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones", indexes = {
    @Index(name = "idx_notificacion_destino_fecha", columnList = "tipo_destino,id_destino,fecha_hora,id")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_evento", nullable = false, length = 36, unique = true)
    private String idEvento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoNotificacion tipo;

    @Column(nullable = false, length = 24)
    private String accion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_destino", nullable = false, length = 32)
    private TipoDestinoNotificacion tipoDestino;

    @Column(name = "id_destino", nullable = false)
    private Long idDestino;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String resumen;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;
}
