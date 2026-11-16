package cl.siga.msreportes.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import cl.siga.coreshare.dto.notificaciones.EventoRiesgoAsistencia;
import cl.siga.coreshare.dto.notificaciones.NivelRiesgo;
import cl.siga.coreshare.dto.notificaciones.TipoRiesgo;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import cl.siga.coreshare.mensajeria.outbox.NotificationEventOutbox;
import cl.siga.msreportes.model.entity.AlertaRiesgo;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class PublicadorAlertaRiesgoTest {

    @Test
    void publicaAlertaDeAsistenciaEnOutboxConRoutingKeyCorrecta() {
        NotificationEventOutbox outbox = Mockito.mock(NotificationEventOutbox.class);
        PublicadorAlertaRiesgo publicador = new PublicadorAlertaRiesgo(outbox);
        AlertaRiesgo alerta = AlertaRiesgo.builder()
            .claveIdempotencia("ASISTENCIA|evento-1|40|CRITICO")
            .tipoRiesgo(TipoRiesgo.INASISTENCIA)
            .idEstudiante(10L)
            .idCursoAsignatura(20L)
            .nivelNuevo(NivelRiesgo.CRITICO)
            .motivo("Superó el umbral")
            .idEvento("evento-1")
            .correlationId("corr-1")
            .creadaEn(LocalDateTime.now())
            .porcentajeInasistencia(45.0)
            .build();

        publicador.publicar(alerta);

        ArgumentCaptor<Object> evento = ArgumentCaptor.forClass(Object.class);
        verify(outbox).registrar(any(String.class), eq(NombresMensajeria.INTERCAMBIO_NOTIFICACIONES),
            eq(NombresMensajeria.CLAVE_RIESGO_INASISTENCIA), evento.capture());
        assertThat(evento.getValue()).isInstanceOf(EventoRiesgoAsistencia.class);
        assertThat(((EventoRiesgoAsistencia) evento.getValue()).porcentajeInasistencia()).isEqualTo(45.0);
    }
}
