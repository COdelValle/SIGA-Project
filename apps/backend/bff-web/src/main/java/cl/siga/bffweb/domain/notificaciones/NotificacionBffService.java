package cl.siga.bffweb.domain.notificaciones;

import cl.siga.bffweb.integration.notificaciones.NotificacionClient;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.notificaciones.ContadorNotificacionesDTO;
import cl.siga.coreshare.dto.notificaciones.NotificacionResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificacionBffService {

    private final NotificacionClient notificacionClient;

    public PageResponseDTO<NotificacionResponseDTO> obtenerMisNotificaciones(int pagina, int tamano) {
        return notificacionClient.obtenerMisNotificaciones(pagina, tamano);
    }

    public ContadorNotificacionesDTO contarNoLeidas() {
        return notificacionClient.contarNoLeidas();
    }

    public void marcarLeida(Long id) {
        notificacionClient.marcarLeida(id);
    }

    public void marcarTodasLeidas() {
        notificacionClient.marcarTodasLeidas();
    }

    public void limpiarLeidas() {
        notificacionClient.limpiarLeidas();
    }
}
