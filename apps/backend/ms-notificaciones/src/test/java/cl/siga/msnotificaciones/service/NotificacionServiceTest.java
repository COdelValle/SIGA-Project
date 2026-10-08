package cl.siga.msnotificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.siga.coreshare.dto.asistencia.enums.Justificacion;
import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.coreshare.dto.notificaciones.AccionAsistencia;
import cl.siga.coreshare.dto.notificaciones.AccionEvaluacion;
import cl.siga.coreshare.dto.notificaciones.AccionNota;
import cl.siga.coreshare.dto.notificaciones.EventoAsistencia;
import cl.siga.coreshare.dto.notificaciones.EventoEvaluacion;
import cl.siga.coreshare.dto.notificaciones.EventoNota;
import cl.siga.coreshare.dto.notificaciones.TipoDestinoNotificacion;
import cl.siga.coreshare.dto.notificaciones.TipoNotificacion;
import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.mensajeria.EventoInvalidoException;
import cl.siga.msnotificaciones.model.entity.Notificacion;
import cl.siga.msnotificaciones.model.entity.NotificacionLectura;
import cl.siga.msnotificaciones.repository.NotificacionLecturaRepository;
import cl.siga.msnotificaciones.repository.NotificacionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class NotificacionServiceTest {

    private NotificacionRepository notificacionRepository;
    private NotificacionLecturaRepository lecturaRepository;
    private ResolverDestinatariosNotificacion resolver;
    private NotificacionService service;

    @BeforeEach
    void setUp() {
        notificacionRepository = mock(NotificacionRepository.class);
        lecturaRepository = mock(NotificacionLecturaRepository.class);
        resolver = mock(ResolverDestinatariosNotificacion.class);
        service = new NotificacionService(notificacionRepository, lecturaRepository, resolver);
    }

    @Test
    void eventoNotaGuardaTituloConAsignaturaYSinPuntajeEnElResumen() {
        EventoNota evento = new EventoNota(17L, 41L, 8L, 6.7, AccionNota.CREADA,
            LocalDateTime.of(2026, 10, 7, 10, 30), "4f0ad118-f4f1-42f5-8354-611c61983164",
            "PRUEBA 1", "Matemática");
        when(notificacionRepository.insertarSiNuevo(
            anyString(), eq("NOTA"), eq("CREADA"), eq("ESTUDIANTE"), eq(41L),
            eq("Calificación agregada en Matemática"), anyString(), any(LocalDateTime.class))).thenReturn(1);

        service.registrarEvento(evento);

        var resumen = ArgumentCaptor.forClass(String.class);
        verify(notificacionRepository).insertarSiNuevo(
            anyString(), eq("NOTA"), eq("CREADA"), eq("ESTUDIANTE"), eq(41L),
            eq("Calificación agregada en Matemática"), resumen.capture(), any(LocalDateTime.class));
        assertThat(resumen.getValue())
            .startsWith("Se agregó una calificación")
            .contains("PRUEBA 1")
            .contains("Matemática")
            .doesNotContain("6.7");
    }

    @Test
    void eventoAsistenciaIncluyeFechaYPorcentajeCuandoSuperaElUmbral() {
        EventoAsistencia evento = new EventoAsistencia(2L, 41L, 8L, LocalDate.of(2026, 10, 7),
            State.AUSENTE, Justificacion.PENDIENTE, AccionAsistencia.REGISTRADA, 60.0, true,
            LocalDateTime.of(2026, 10, 7, 8, 0), "5f0ad118-f4f1-42f5-8354-611c61983164", "Matemática");
        when(notificacionRepository.insertarSiNuevo(
            anyString(), eq("ASISTENCIA"), eq("REGISTRADA"), eq("ESTUDIANTE"), eq(41L),
            eq("Registro de ausente en Matemática"), anyString(), any(LocalDateTime.class))).thenReturn(1);

        service.registrarEvento(evento);

        var resumen = ArgumentCaptor.forClass(String.class);
        verify(notificacionRepository).insertarSiNuevo(
            anyString(), eq("ASISTENCIA"), eq("REGISTRADA"), eq("ESTUDIANTE"), eq(41L),
            eq("Registro de ausente en Matemática"), resumen.capture(), any(LocalDateTime.class));
        assertThat(resumen.getValue())
            .contains("07/10/2026")
            .contains("60%");
    }

    @Test
    void obtenerBandejaMarcaComoLeidaSoloParaElOidActual() {
        Notificacion notificacion = Notificacion.builder()
            .id(12L).idEvento("evt-12").tipo(TipoNotificacion.NOTA).accion("CREADA")
            .tipoDestino(TipoDestinoNotificacion.ESTUDIANTE).idDestino(41L)
            .titulo("Nueva calificación").resumen("Hay una nueva calificación disponible en SIGA.")
            .fechaHora(LocalDateTime.of(2026, 10, 7, 10, 30)).build();
        when(resolver.resolverActual()).thenReturn(new DestinatariosNotificacion("oid-41", Set.of(41L), Set.of(81L)));
        when(notificacionRepository.buscarVisibles(
            eq(true), eq(TipoDestinoNotificacion.ESTUDIANTE), anyList(),
            eq(true), eq(TipoDestinoNotificacion.CURSO_ASIGNATURA), anyList(),
            eq("oid-41"), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(notificacion), PageRequest.of(0, 10), 1));
        when(lecturaRepository.buscarIdsLeidos("oid-41", List.of(12L))).thenReturn(List.of(12L));

        var resultado = service.obtenerMisNotificaciones(0, 10);

        assertThat(resultado.getContent()).singleElement()
            .satisfies(item -> {
                assertThat(item.id()).isEqualTo(12L);
                assertThat(item.leida()).isTrue();
            });
    }

    @Test
    void marcarLeidaNoPermiteUsarUnaNotificacionDeOtroEstudiante() {
        Notificacion ajena = Notificacion.builder()
            .id(22L).idEvento("evt-22").tipo(TipoNotificacion.ASISTENCIA).accion("REGISTRADA")
            .tipoDestino(TipoDestinoNotificacion.ESTUDIANTE).idDestino(41L)
            .titulo("Registro de ausente").resumen("Revisa SIGA.")
            .fechaHora(LocalDateTime.now()).build();
        when(notificacionRepository.findById(22L)).thenReturn(Optional.of(ajena));
        when(resolver.resolverActual()).thenReturn(new DestinatariosNotificacion("oid-otra", Set.of(77L), Set.of()));

        assertThatThrownBy(() -> service.marcarLeida(22L))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(lecturaRepository, never()).marcarLeidaSiNoExiste(any(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void contadorYMarcaDeLecturaSeLimitanAlOidAutenticado() {
        DestinatariosNotificacion destinatarios =
            new DestinatariosNotificacion("oid-41", Set.of(41L), Set.of(81L));
        when(resolver.resolverActual()).thenReturn(destinatarios);
        when(notificacionRepository.contarNoLeidas(
            eq(true), eq(TipoDestinoNotificacion.ESTUDIANTE), anyList(),
            eq(true), eq(TipoDestinoNotificacion.CURSO_ASIGNATURA), anyList(), eq("oid-41")))
            .thenReturn(2L);

        assertThat(service.contarNoLeidas().noLeidas()).isEqualTo(2L);

        Notificacion propia = Notificacion.builder()
            .id(31L).idEvento("evt-31").tipo(TipoNotificacion.EVALUACION).accion("CREADA")
            .tipoDestino(TipoDestinoNotificacion.CURSO_ASIGNATURA).idDestino(81L)
            .titulo("Nueva evaluación").resumen("Se agregó una evaluación.")
            .fechaHora(LocalDateTime.now()).build();
        when(notificacionRepository.findById(31L)).thenReturn(Optional.of(propia));

        service.marcarLeida(31L);

        verify(lecturaRepository).marcarLeidaSiNoExiste(eq(31L), eq("oid-41"), any(LocalDateTime.class));
    }

    @Test
    void marcarTodasLeidasInsertaSoloLasPendientesDelOid() {
        when(resolver.resolverActual()).thenReturn(
            new DestinatariosNotificacion("oid-41", Set.of(41L), Set.of(81L)));
        when(notificacionRepository.idsVisiblesNoLeidas(
            eq(true), eq(TipoDestinoNotificacion.ESTUDIANTE), anyList(),
            eq(true), eq(TipoDestinoNotificacion.CURSO_ASIGNATURA), anyList(), eq("oid-41")))
            .thenReturn(List.of(12L, 13L));

        int marcadas = service.marcarTodasLeidas();

        assertThat(marcadas).isEqualTo(2);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NotificacionLectura>> captor = ArgumentCaptor.forClass(List.class);
        verify(lecturaRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
            .extracting(lectura -> lectura.getId().getIdUsuario())
            .containsOnly("oid-41");
    }

    @Test
    void limpiarLeidasOcultaSoloLasDelOidActual() {
        when(resolver.resolverActual()).thenReturn(
            new DestinatariosNotificacion("oid-41", Set.of(41L), Set.of(81L)));
        when(lecturaRepository.ocultarLeidas(eq("oid-41"), any(LocalDateTime.class))).thenReturn(3);

        assertThat(service.limpiarLeidas()).isEqualTo(3);

        verify(lecturaRepository).ocultarLeidas(eq("oid-41"), any(LocalDateTime.class));
    }

    @Test
    void purgarAntiguasUsaLaRetencionIndicada() {
        when(notificacionRepository.eliminarAnterioresA(any(LocalDateTime.class))).thenReturn(5);

        assertThat(service.purgarAntiguas(90)).isEqualTo(5);
        assertThat(service.purgarAntiguas(0)).isZero();
    }

    @Test
    void eventoNotaSinEstudianteSeRechazaComoInvalido() {
        EventoNota evento = new EventoNota(17L, null, 8L, 6.7, AccionNota.CREADA,
            LocalDateTime.of(2026, 10, 7, 10, 30), "evt-invalido", "PRUEBA 1", "Matemática");

        assertThatThrownBy(() -> service.registrarEvento(evento))
            .isInstanceOf(EventoInvalidoException.class);
    }

    @Test
    void eventoEvaluacionSinDictacionSeRechazaComoInvalido() {
        EventoEvaluacion evento = new EventoEvaluacion(3L, "PRUEBA", TipoEvaluacion.SUMATIVA, 30.0,
            null, AccionEvaluacion.CREADA, LocalDateTime.of(2026, 10, 7, 10, 30), "evt-invalido", null);

        assertThatThrownBy(() -> service.registrarEvento(evento))
            .isInstanceOf(EventoInvalidoException.class);
    }
}
