package cl.siga.bffweb.integration.notificaciones;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.notificaciones.ContadorNotificacionesDTO;
import cl.siga.coreshare.dto.notificaciones.NotificacionResponseDTO;
import cl.siga.coreshare.exception.FeignFallbacks;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificacionClientFallbackFactory implements FallbackFactory<NotificacionClient> {

    @Override
    public NotificacionClient create(Throwable causa) {
        return new NotificacionClient() {
            @Override
            public PageResponseDTO<NotificacionResponseDTO> obtenerMisNotificaciones(int pagina, int tamano) {
                throw error("No se pudieron obtener las notificaciones.");
            }

            @Override
            public ContadorNotificacionesDTO contarNoLeidas() {
                throw error("No se pudo obtener el contador de notificaciones.");
            }

            @Override
            public void marcarLeida(Long id) {
                throw error("No se pudo actualizar la notificación.");
            }

            @Override
            public void marcarTodasLeidas() {
                throw error("No se pudieron marcar las notificaciones como leídas.");
            }

            @Override
            public void limpiarLeidas() {
                throw error("No se pudieron limpiar las notificaciones leídas.");
            }

            private RuntimeException error(String mensaje) {
                log.error("Fallo al llamar a ms-notificaciones (HTTP {}): {}",
                    causa instanceof FeignException feign ? feign.status() : -1, causa.getMessage());
                return FeignFallbacks.noDisponible(causa, mensaje);
            }
        };
    }
}
