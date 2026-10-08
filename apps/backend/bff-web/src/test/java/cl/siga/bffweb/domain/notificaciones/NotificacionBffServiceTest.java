package cl.siga.bffweb.domain.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.siga.bffweb.integration.notificaciones.NotificacionClient;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.notificaciones.ContadorNotificacionesDTO;
import cl.siga.coreshare.dto.notificaciones.NotificacionResponseDTO;
import cl.siga.coreshare.dto.notificaciones.TipoNotificacion;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificacionBffServiceTest {

    private final NotificacionClient client = mock(NotificacionClient.class);
    private final NotificacionBffService service = new NotificacionBffService(client);

    @Test
    void consultaBandejaYContadorSinAceptarUnOidDelCliente() {
        var notificacion = new NotificacionResponseDTO(1L, TipoNotificacion.NOTA, "CREADA",
            "Nueva calificación", "Hay una nueva calificación disponible en SIGA.",
            LocalDateTime.of(2026, 10, 7, 10, 0), false);
        var pagina = new PageResponseDTO<>(List.of(notificacion), 1, 1, 10, 0, true, true, false);
        when(client.obtenerMisNotificaciones(0, 10)).thenReturn(pagina);
        when(client.contarNoLeidas()).thenReturn(new ContadorNotificacionesDTO(1));

        assertThat(service.obtenerMisNotificaciones(0, 10)).isEqualTo(pagina);
        assertThat(service.contarNoLeidas().noLeidas()).isEqualTo(1);
        verify(client).obtenerMisNotificaciones(0, 10);
        verify(client).contarNoLeidas();
    }

    @Test
    void marcaComoLeidaPorIdentificadorDeNotificacion() {
        service.marcarLeida(42L);

        verify(client).marcarLeida(42L);
    }

    @Test
    void marcaTodasYLimpiaLeidasDelegandoEnElServicio() {
        service.marcarTodasLeidas();
        service.limpiarLeidas();

        verify(client).marcarTodasLeidas();
        verify(client).limpiarLeidas();
    }
}
