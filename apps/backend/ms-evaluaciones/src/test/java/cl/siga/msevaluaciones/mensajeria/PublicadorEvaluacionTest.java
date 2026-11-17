package cl.siga.msevaluaciones.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import cl.siga.coreshare.dto.notificaciones.EventoEvaluacion;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.mensajeria.NotificacionesMensajeriaProperties;
import cl.siga.coreshare.mensajeria.outbox.NotificationEventOutbox;
import cl.siga.msevaluaciones.client.AsignaturaClient;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PublicadorEvaluacionTest {

    private final NotificacionesMensajeriaProperties propiedades = propiedades();

    @Test
    void registraEvaluacionEnOutboxConLaClaveYAsignatura() {
        NotificationEventOutbox outbox = mock(NotificationEventOutbox.class);
        AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);
        when(asignaturaClient.getCursoAsignaturaById(8L)).thenReturn(
            new CursoAsignaturaResponseDTO(
                8L, 1L, "Matemática", "matemática", AreaAcademica.MATEMATICAS, true,
                CaracterAsignatura.OBLIGATORIA, Semestre.SEMESTRE_1, 6L, 4L, null, null, 0, List.of()));
        PublicadorEvaluacion publicador = new PublicadorEvaluacion(outbox, asignaturaClient, propiedades);

        publicador.publicarCreada(new EvaluacionResponseDTO(3L, "PRUEBA", TipoEvaluacion.SUMATIVA,
            30.0, 8L, true));

        ArgumentCaptor<EventoEvaluacion> evento = ArgumentCaptor.forClass(EventoEvaluacion.class);
        verify(outbox).registrar(anyString(), eq(propiedades.getExchangeNotificaciones()),
            eq(propiedades.getClaveEvaluacionCreada()), evento.capture());
        assertThat(UUID.fromString(evento.getValue().idEvento())).isNotNull();
        assertThat(evento.getValue().idCursoAsignatura()).isEqualTo(8L);
        assertThat(evento.getValue().nombreAsignatura()).isEqualTo("Matemática");
    }

    @Test
    void publicaAunqueFalleElNombreDeLaAsignatura() {
        NotificationEventOutbox outbox = mock(NotificationEventOutbox.class);
        AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);
        when(asignaturaClient.getCursoAsignaturaById(8L)).thenThrow(new RuntimeException("ms caído"));
        PublicadorEvaluacion publicador = new PublicadorEvaluacion(outbox, asignaturaClient, propiedades);

        publicador.publicarEliminada(new EvaluacionResponseDTO(3L, "PRUEBA", TipoEvaluacion.SUMATIVA,
            30.0, 8L, false));

        ArgumentCaptor<EventoEvaluacion> evento = ArgumentCaptor.forClass(EventoEvaluacion.class);
        verify(outbox).registrar(anyString(), eq(propiedades.getExchangeNotificaciones()),
            eq(propiedades.getClaveEvaluacionEliminada()), evento.capture());
        assertThat(evento.getValue().nombreAsignatura()).isNull();
    }

    @Test
    void publicarActualizadaUsaLaClaveDeActualizacion() {
        NotificationEventOutbox outbox = mock(NotificationEventOutbox.class);
        AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);
        when(asignaturaClient.getCursoAsignaturaById(8L)).thenReturn(
            new CursoAsignaturaResponseDTO(
                8L, 1L, "Matemática", "matemática", AreaAcademica.MATEMATICAS, true,
                CaracterAsignatura.OBLIGATORIA, Semestre.SEMESTRE_1, 6L, 4L, null, null, 0, List.of()));
        PublicadorEvaluacion publicador = new PublicadorEvaluacion(outbox, asignaturaClient, propiedades);

        publicador.publicarActualizada(new EvaluacionResponseDTO(3L, "PRUEBA", TipoEvaluacion.SUMATIVA,
            30.0, 8L, true));

        ArgumentCaptor<EventoEvaluacion> evento = ArgumentCaptor.forClass(EventoEvaluacion.class);
        verify(outbox).registrar(anyString(), eq(propiedades.getExchangeNotificaciones()),
            eq(propiedades.getClaveEvaluacionActualizada()), evento.capture());
        assertThat(evento.getValue().nombreAsignatura()).isEqualTo("Matemática");
    }

    @Test
    void noConsultaLaDictacionCuandoLaEvaluacionNoTieneCursoAsignatura() {
        NotificationEventOutbox outbox = mock(NotificationEventOutbox.class);
        AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);
        PublicadorEvaluacion publicador = new PublicadorEvaluacion(outbox, asignaturaClient, propiedades);

        publicador.publicarCreada(new EvaluacionResponseDTO(3L, "PRUEBA", TipoEvaluacion.FORMATIVA,
            0.0, null, true));

        ArgumentCaptor<EventoEvaluacion> evento = ArgumentCaptor.forClass(EventoEvaluacion.class);
        verify(outbox).registrar(anyString(), eq(propiedades.getExchangeNotificaciones()),
            eq(propiedades.getClaveEvaluacionCreada()), evento.capture());
        assertThat(evento.getValue().nombreAsignatura()).isNull();
        verify(asignaturaClient, org.mockito.Mockito.never()).getCursoAsignaturaById(any());
    }

    @Test
    void publicaConNombreNuloCuandoLaDictacionNoExiste() {
        NotificationEventOutbox outbox = mock(NotificationEventOutbox.class);
        AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);
        when(asignaturaClient.getCursoAsignaturaById(8L)).thenReturn(null);
        PublicadorEvaluacion publicador = new PublicadorEvaluacion(outbox, asignaturaClient, propiedades);

        publicador.publicarCreada(new EvaluacionResponseDTO(3L, "PRUEBA", TipoEvaluacion.SUMATIVA,
            30.0, 8L, true));

        ArgumentCaptor<EventoEvaluacion> evento = ArgumentCaptor.forClass(EventoEvaluacion.class);
        verify(outbox).registrar(anyString(), eq(propiedades.getExchangeNotificaciones()),
            eq(propiedades.getClaveEvaluacionCreada()), evento.capture());
        assertThat(evento.getValue().nombreAsignatura()).isNull();
    }

    private static NotificacionesMensajeriaProperties propiedades() {
        NotificacionesMensajeriaProperties p = new NotificacionesMensajeriaProperties();
        p.setExchangeNotificaciones("intercambio-notificaciones");
        p.setClaveEvaluacionCreada("evaluacion.creada");
        p.setClaveEvaluacionActualizada("evaluacion.actualizada");
        p.setClaveEvaluacionEliminada("evaluacion.eliminada");
        return p;
    }
}
