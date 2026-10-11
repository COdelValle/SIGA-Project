package cl.siga.msnotificaciones.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion_lecturas", indexes = {
    @Index(name = "idx_notificacion_lectura_usuario", columnList = "id_usuario,leida_en")
})
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NotificacionLectura {

    @EmbeddedId
    private NotificacionLecturaId id;

    @Column(name = "leida_en", nullable = false)
    private LocalDateTime leidaEn;

    /** No nulo = el usuario la ocultó de su bandeja ("Limpiar mis leídas"). */
    @Column(name = "ocultada_en")
    private LocalDateTime ocultadaEn;
}
