package cl.siga.msnotificaciones.service;

import java.util.Set;

public record DestinatariosNotificacion(
    String idUsuario,
    Set<Long> idEstudiantes,
    Set<Long> idDictaciones
) {
    public boolean incluyeEstudiantes() {
        return !idEstudiantes.isEmpty();
    }

    public boolean incluyeDictaciones() {
        return !idDictaciones.isEmpty();
    }
}
