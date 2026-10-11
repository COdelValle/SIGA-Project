package cl.siga.msnotas.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;
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
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.dto.notificaciones.EventoNota;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import cl.siga.coreshare.mensajeria.outbox.NotificationEventOutbox;
import cl.siga.msnotas.client.AsignaturaClient;
import cl.siga.msnotas.client.EvaluacionClient;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PublicadorNotaTest {

    @Test
    void registraEventoEnOutboxConClaveDeCreacionYNombreDeAsignatura() {
        NotificationEventOutbox outbox = mock(NotificationEventOutbox.class);
        EvaluacionClient evaluacionClient = mock(EvaluacionClient.class);
        AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);
        when(evaluacionClient.getEvaluacionById(12L)).thenReturn(
            new EvaluacionResponseDTO(12L, "PRUEBA 1", TipoEvaluacion.SUMATIVA, 30.0, 8L, true));
        when(asignaturaClient.getCursoAsignaturaById(8L)).thenReturn(dictacion("Matemática"));
        PublicadorNota publicador = new PublicadorNota(outbox, evaluacionClient, asignaturaClient);

        publicador.publicarCreada(new NotaResponseDTO(4L, 10L, 12L, 6.0));

        ArgumentCaptor<EventoNota> evento = ArgumentCaptor.forClass(EventoNota.class);
        verify(outbox).registrar(anyString(), eq(NombresMensajeria.INTERCAMBIO_NOTIFICACIONES),
            eq(NombresMensajeria.CLAVE_NOTA_CREADA), evento.capture());
        assertThat(UUID.fromString(evento.getValue().idEvento())).isNotNull();
        assertThat(evento.getValue().idEstudiante()).isEqualTo(10L);
        assertThat(evento.getValue().nombreEvaluacion()).isEqualTo("PRUEBA 1");
        assertThat(evento.getValue().nombreAsignatura()).isEqualTo("Matemática");
    }

    @Test
    void publicaElEventoAunqueElEnriquecimientoFalle() {
        NotificationEventOutbox outbox = mock(NotificationEventOutbox.class);
        EvaluacionClient evaluacionClient = mock(EvaluacionClient.class);
        AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);
        when(evaluacionClient.getEvaluacionById(12L)).thenThrow(new RuntimeException("ms caído"));
        PublicadorNota publicador = new PublicadorNota(outbox, evaluacionClient, asignaturaClient);

        publicador.publicarActualizada(new NotaResponseDTO(4L, 10L, 12L, 6.0));

        ArgumentCaptor<EventoNota> evento = ArgumentCaptor.forClass(EventoNota.class);
        verify(outbox).registrar(anyString(), eq(NombresMensajeria.INTERCAMBIO_NOTIFICACIONES),
            eq(NombresMensajeria.CLAVE_NOTA_ACTUALIZADA), evento.capture());
        assertThat(evento.getValue().nombreEvaluacion()).isNull();
        assertThat(evento.getValue().nombreAsignatura()).isNull();
    }

    private static CursoAsignaturaResponseDTO dictacion(String nombre) {
        return new CursoAsignaturaResponseDTO(
            8L, 1L, nombre, nombre.toLowerCase(), AreaAcademica.MATEMATICAS, true,
            CaracterAsignatura.OBLIGATORIA, Semestre.SEMESTRE_1, 6L, 4L, null, null, 0, List.of());
    }
}
