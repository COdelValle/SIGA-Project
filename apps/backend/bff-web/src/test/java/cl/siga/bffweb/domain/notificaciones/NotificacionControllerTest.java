package cl.siga.bffweb.domain.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.notificaciones.ContadorNotificacionesDTO;
import cl.siga.coreshare.dto.notificaciones.NotificacionResponseDTO;

class NotificacionControllerTest {

    private final NotificacionBffService service = mock(NotificacionBffService.class);
    private final NotificacionController controller = new NotificacionController(service);

    @Test
    void bandejaDelUsuarioDelegaEnElServicio() {
        PageResponseDTO<NotificacionResponseDTO> pagina =
            new PageResponseDTO<>(List.of(), 0, 0, 10, 0, true, true, true);
        when(service.obtenerMisNotificaciones(0, 10)).thenReturn(pagina);

        var respuesta = controller.obtenerMisNotificaciones(0, 10);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).isSameAs(pagina);
        verify(service).obtenerMisNotificaciones(0, 10);
    }

    @Test
    void contadorDeNoLeidasDelegaEnElServicio() {
        ContadorNotificacionesDTO contador = new ContadorNotificacionesDTO(3);
        when(service.contarNoLeidas()).thenReturn(contador);

        var respuesta = controller.contarNoLeidas();

        assertThat(respuesta.getBody()).isSameAs(contador);
        verify(service).contarNoLeidas();
    }

    @Test
    void marcarLeidaResponde204() {
        var respuesta = controller.marcarLeida(42L);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(204);
        verify(service).marcarLeida(42L);
    }

    @Test
    void marcarTodasLasNotificacionesResponde204() {
        var respuesta = controller.marcarTodasLeidas();

        assertThat(respuesta.getStatusCode().value()).isEqualTo(204);
        verify(service).marcarTodasLeidas();
    }

    @Test
    void limpiarLeidasResponde204() {
        var respuesta = controller.limpiarLeidas();

        assertThat(respuesta.getStatusCode().value()).isEqualTo(204);
        verify(service).limpiarLeidas();
    }
}
