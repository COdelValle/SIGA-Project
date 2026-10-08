package cl.siga.msnotificaciones.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NotificacionLecturaId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_notificacion", nullable = false)
    private Long idNotificacion;

    @Column(name = "id_usuario", nullable = false, length = 36)
    private String idUsuario;
}
