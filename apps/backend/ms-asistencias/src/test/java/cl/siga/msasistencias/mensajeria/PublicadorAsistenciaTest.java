package cl.siga.msasistencias.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.asistencia.enums.Justificacion;
import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.coreshare.dto.notificaciones.EventoAsistencia;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import cl.siga.coreshare.mensajeria.outbox.NotificationEventOutbox;
import cl.siga.msasistencias.client.AsignaturaClient;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PublicadorAsistenciaTest {

    @Test
    void registraAsistenciaEnOutboxConElUmbralYLaAsignatura() {
        NotificationEventOutbox outbox = mock(NotificationEventOutbox.class);
        AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);
        when(asignaturaClient.getCursoAsignaturaById(4L)).thenReturn(
            new CursoAsignaturaResponseDTO(
                4L, 1L, "Matemática", "matemática", AreaAcademica.MATEMATICAS, true,
                CaracterAsignatura.OBLIGATORIA, Semestre.SEMESTRE_1, 6L, 8L, null, null, 0, List.of()));
        PublicadorAsistencia publicador = new PublicadorAsistencia(outbox, asignaturaClient);
        AsistenciaResponseDTO asistencia = new AsistenciaResponseDTO(2L, 10L, 4L,
            LocalDate.of(2026, 10, 7), State.AUSENTE, Justificacion.PENDIENTE, null);

        publicador.publicarRegistrada(asistencia, 60.0, true);

        ArgumentCaptor<EventoAsistencia> evento = ArgumentCaptor.forClass(EventoAsistencia.class);
        verify(outbox).registrar(anyString(), eq(NombresMensajeria.INTERCAMBIO_NOTIFICACIONES),
            eq(NombresMensajeria.CLAVE_ASISTENCIA_REGISTRADA), evento.capture());
        assertThat(UUID.fromString(evento.getValue().idEvento())).isNotNull();
        assertThat(evento.getValue().porcentajeInasistencia()).isEqualTo(60.0);
        assertThat(evento.getValue().superaUmbralInasistencia()).isTrue();
        assertThat(evento.getValue().nombreAsignatura()).isEqualTo("Matemática");
    }
}
