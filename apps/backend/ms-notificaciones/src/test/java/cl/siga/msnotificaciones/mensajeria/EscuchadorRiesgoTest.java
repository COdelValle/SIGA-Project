package cl.siga.msnotificaciones.mensajeria;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import cl.siga.coreshare.dto.notificaciones.EventoRiesgoAsistencia;
import cl.siga.coreshare.dto.notificaciones.NivelRiesgo;
import cl.siga.coreshare.dto.notificaciones.TipoRiesgo;
import cl.siga.coreshare.mensajeria.ConfirmadorMensajes;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import cl.siga.msnotificaciones.service.NotificacionService;
import com.rabbitmq.client.Channel;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EscuchadorRiesgoTest {

    @Test
    void entregaAlServicioLaAlertaDeInasistenciaYUsaSuCola() {
        NotificacionService service = Mockito.mock(NotificacionService.class);
        ConfirmadorMensajes confirmador = Mockito.mock(ConfirmadorMensajes.class);
        EscuchadorRiesgo escuchador = new EscuchadorRiesgo(service, confirmador);
        EventoRiesgoAsistencia evento = new EventoRiesgoAsistencia(
            10L, 20L, NivelRiesgo.CRITICO, TipoRiesgo.INASISTENCIA, 45.0,
            "Superó el umbral", LocalDateTime.now(), "evento-1", "corr-1");

        Channel canal = Mockito.mock(Channel.class);
        escuchador.recibirInasistencia(evento, canal, 7L);

        var operacion = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(confirmador).procesar(eq(canal), eq(7L),
            eq(NombresMensajeria.COLA_RIESGO_INASISTENCIA), operacion.capture());
        operacion.getValue().run();
        verify(service).registrarEvento(evento);
    }
}
