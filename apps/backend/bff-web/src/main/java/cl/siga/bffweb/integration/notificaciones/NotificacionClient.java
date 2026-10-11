package cl.siga.bffweb.integration.notificaciones;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.notificaciones.ContadorNotificacionesDTO;
import cl.siga.coreshare.dto.notificaciones.NotificacionResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "ms-notificaciones",
    url = "${services.notificaciones.url}",
    fallbackFactory = NotificacionClientFallbackFactory.class
)
public interface NotificacionClient {

    @GetMapping("/api/v1/notificaciones/me")
    PageResponseDTO<NotificacionResponseDTO> obtenerMisNotificaciones(
        @RequestParam("page") int pagina,
        @RequestParam("size") int tamano);

    @GetMapping("/api/v1/notificaciones/me/no-leidas/count")
    ContadorNotificacionesDTO contarNoLeidas();

    @PatchMapping("/api/v1/notificaciones/me/{id}/leida")
    void marcarLeida(@PathVariable("id") Long id);

    @PatchMapping("/api/v1/notificaciones/me/leidas")
    void marcarTodasLeidas();

    @DeleteMapping("/api/v1/notificaciones/me/leidas")
    void limpiarLeidas();
}
