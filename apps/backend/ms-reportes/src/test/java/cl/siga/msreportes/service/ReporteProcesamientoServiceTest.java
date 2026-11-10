package cl.siga.msreportes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.coreshare.dto.kafka.EventoAsistenciaKafka;
import cl.siga.coreshare.dto.kafka.EventoRendimientoKafka;
import cl.siga.coreshare.dto.notificaciones.NivelRiesgo;
import cl.siga.msreportes.model.entity.AlertaRiesgo;
import cl.siga.msreportes.model.entity.EventoReporteProcesado;
import cl.siga.msreportes.model.entity.ProyeccionAsistencia;
import cl.siga.msreportes.model.entity.ProyeccionRendimiento;
import cl.siga.msreportes.mensajeria.PublicadorAlertaRiesgo;
import cl.siga.msreportes.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validator;

class ReporteProcesamientoServiceTest {

    private static Validator validator;
    private static ValidatorFactory validatorFactory;

    @BeforeAll
    static void crearValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void cerrarValidator() {
        validatorFactory.close();
    }

    @Test
    void rechazaPayloadInvalidoAntesDePersistir() {
        ReporteProcesamientoService service = servicio();
        EventoAsistenciaKafka invalido = new EventoAsistenciaKafka(
            null, 10L, 4L, LocalDate.now(), null, null,
            20.0, 1, 1, LocalDateTime.now(), "", "");

        assertThatThrownBy(() -> service.procesarAsistencia(
            new ConsumerRecord<>("siga.academico.asistencias", 0, 1L, "10", invalido)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Payload Kafka inválido");
    }

    @Test
    void ignoraEventoDuplicadoSinModificarProyeccion() {
        EventoReporteProcesadoRepository eventos = mock(EventoReporteProcesadoRepository.class);
        when(eventos.existsByIdEvento("evento-1")).thenReturn(true);
        ProyeccionAsistenciaRepository asistencias = mock(ProyeccionAsistenciaRepository.class);
        ReporteProcesamientoService service = new ReporteProcesamientoService(
            validator, eventos, asistencias, mock(ProyeccionRendimientoRepository.class),
            mock(AlertaRiesgoRepository.class));
        EventoAsistenciaKafka evento = new EventoAsistenciaKafka(
            1L, 10L, 4L, LocalDate.now(), cl.siga.coreshare.dto.asistencia.enums.State.AUSENTE,
            null, 60.0, 10, 6, LocalDateTime.now(), "evento-1", "corr-1");

        service.procesarAsistencia(
            new ConsumerRecord<>("siga.academico.asistencias", 0, 1L, "10", evento));

        verify(asistencias, never()).save(any());
    }

    @Test
    void ignoraEventoDeRendimientoDuplicadoSinModificarProyeccion() {
        EventoReporteProcesadoRepository eventos = mock(EventoReporteProcesadoRepository.class);
        when(eventos.existsByIdEvento("evento-r")).thenReturn(true);
        ProyeccionRendimientoRepository rendimiento = mock(ProyeccionRendimientoRepository.class);
        ReporteProcesamientoService service = new ReporteProcesamientoService(
            validator, eventos, mock(ProyeccionAsistenciaRepository.class), rendimiento,
            mock(AlertaRiesgoRepository.class));
        EventoRendimientoKafka evento = new EventoRendimientoKafka(
            2L, 10L, 30L, 20L, 3.5, null, 2,
            LocalDateTime.now(), "evento-r", "corr-r");

        service.procesarRendimiento(
            new ConsumerRecord<>("siga.academico.rendimiento", 1, 5L, "10", evento));

        verify(rendimiento, never()).save(any());
    }

    @Test
    void persisteRendimientoConCorrelationIdYEventoProcesado() {
        EventoReporteProcesadoRepository eventos = mock(EventoReporteProcesadoRepository.class);
        ProyeccionRendimientoRepository rendimiento = mock(ProyeccionRendimientoRepository.class);
        when(eventos.existsByIdEvento("evento-r-nuevo")).thenReturn(false);
        ReporteProcesamientoService service = new ReporteProcesamientoService(
            validator, eventos, mock(ProyeccionAsistenciaRepository.class), rendimiento,
            mock(AlertaRiesgoRepository.class));
        EventoRendimientoKafka evento = new EventoRendimientoKafka(
            2L, 10L, 30L, 20L, 6.0, null, 0,
            LocalDateTime.now(), "evento-r-nuevo", "corr-r-nuevo");

        service.procesarRendimiento(
            new ConsumerRecord<>("siga.academico.rendimiento", 1, 5L, "10", evento));

        var proyeccion = org.mockito.ArgumentCaptor.forClass(ProyeccionRendimiento.class);
        verify(rendimiento, atLeastOnce()).save(proyeccion.capture());
        org.assertj.core.api.Assertions.assertThat(
            proyeccion.getAllValues().get(proyeccion.getAllValues().size() - 1).getCorrelationId())
            .isEqualTo("corr-r-nuevo");
        var procesado = org.mockito.ArgumentCaptor.forClass(EventoReporteProcesado.class);
        verify(eventos).save(procesado.capture());
        org.assertj.core.api.Assertions.assertThat(procesado.getValue().getCorrelationId())
            .isEqualTo("corr-r-nuevo");
    }

    @Test
    void recalculaPromedioPorEstudianteYAsignatura() {
        EventoReporteProcesadoRepository eventos = mock(EventoReporteProcesadoRepository.class);
        ProyeccionRendimientoRepository rendimiento = mock(ProyeccionRendimientoRepository.class);
        when(eventos.existsByIdEvento("evento-r-promedio")).thenReturn(false);
        ProyeccionRendimiento notaAnterior = ProyeccionRendimiento.builder()
            .id(8L).idEstudiante(10L).idEvaluacion(29L).idCursoAsignatura(20L)
            .idNota(19L).puntaje(6.0).build();
        ReporteProcesamientoService service = new ReporteProcesamientoService(
            validator, eventos, mock(ProyeccionAsistenciaRepository.class), rendimiento,
            mock(AlertaRiesgoRepository.class));
        EventoRendimientoKafka evento = new EventoRendimientoKafka(
            2L, 10L, 30L, 20L, 2.0, null, 0,
            LocalDateTime.now(), "evento-r-promedio", "corr-r-promedio");
        when(rendimiento.findByIdEstudianteAndIdCursoAsignatura(10L, 20L))
            .thenAnswer(invocacion -> List.of(notaAnterior,
                ProyeccionRendimiento.builder().idEstudiante(10L).idEvaluacion(30L)
                    .idCursoAsignatura(20L).idNota(2L).puntaje(2.0).build()));

        service.procesarRendimiento(
            new ConsumerRecord<>("siga.academico.rendimiento", 1, 6L, "10", evento));

        var proyeccion = org.mockito.ArgumentCaptor.forClass(ProyeccionRendimiento.class);
        verify(rendimiento, atLeastOnce()).save(proyeccion.capture());
        ProyeccionRendimiento guardada = proyeccion.getAllValues().get(proyeccion.getAllValues().size() - 1);
        assertThat(guardada.getPromedio()).isEqualTo(4.0);
        assertThat(guardada.getEvaluacionesInsuficientes()).isEqualTo(1L);
    }

    @Test
    void persisteAlertaCuandoLaInasistenciaSuperaUmbral() {
        EventoReporteProcesadoRepository eventos = mock(EventoReporteProcesadoRepository.class);
        ProyeccionAsistenciaRepository asistencias = mock(ProyeccionAsistenciaRepository.class);
        AlertaRiesgoRepository alertas = mock(AlertaRiesgoRepository.class);
        when(alertas.existsByClaveIdempotencia(any())).thenReturn(false);
        ReporteProcesamientoService service = new ReporteProcesamientoService(
            validator, eventos, asistencias, mock(ProyeccionRendimientoRepository.class), alertas);
        EventoAsistenciaKafka evento = new EventoAsistenciaKafka(
            1L, 10L, 4L, LocalDate.now(), cl.siga.coreshare.dto.asistencia.enums.State.AUSENTE,
            null, 60.0, 10, 6, LocalDateTime.now(), "evento-2", "corr-2");

        service.procesarAsistencia(
            new ConsumerRecord<>("siga.academico.asistencias", 0, 2L, "10", evento));

        verify(alertas, atLeastOnce()).save(any(AlertaRiesgo.class));
    }

    @Test
    void noPublicaAlertaParaAsistenciaSinRiesgo() {
        EventoReporteProcesadoRepository eventos = mock(EventoReporteProcesadoRepository.class);
        ProyeccionAsistenciaRepository asistencias = mock(ProyeccionAsistenciaRepository.class);
        AlertaRiesgoRepository alertas = mock(AlertaRiesgoRepository.class);
        PublicadorAlertaRiesgo publicador = mock(PublicadorAlertaRiesgo.class);
        ReporteProcesamientoService service = servicioConPublicador(
            eventos, asistencias, mock(ProyeccionRendimientoRepository.class), alertas, publicador);
        EventoAsistenciaKafka evento = new EventoAsistenciaKafka(
            1L, 10L, 4L, LocalDate.now(), State.PRESENTE, null,
            10.0, 10, 1, LocalDateTime.now(), "evento-normal", "corr-normal");

        service.procesarAsistencia(
            new ConsumerRecord<>("siga.academico.asistencias", 0, 2L, "10", evento));

        verify(publicador, never()).publicar(any(AlertaRiesgo.class));
        verify(alertas, never()).save(any(AlertaRiesgo.class));
    }

    @Test
    void publicaAlertaDeRendimientoSoloCuandoHayRiesgoYConTrazabilidad() {
        EventoReporteProcesadoRepository eventos = mock(EventoReporteProcesadoRepository.class);
        ProyeccionRendimientoRepository rendimiento = mock(ProyeccionRendimientoRepository.class);
        AlertaRiesgoRepository alertas = mock(AlertaRiesgoRepository.class);
        PublicadorAlertaRiesgo publicador = mock(PublicadorAlertaRiesgo.class);
        when(alertas.existsByClaveIdempotencia(any())).thenReturn(false);
        when(rendimiento.findByIdEstudianteAndIdCursoAsignatura(10L, 20L))
            .thenReturn(java.util.List.of());
        ReporteProcesamientoService service = servicioConPublicador(
            eventos, mock(ProyeccionAsistenciaRepository.class), rendimiento, alertas, publicador);
        EventoRendimientoKafka evento = new EventoRendimientoKafka(
            2L, 10L, 30L, 20L, 3.0, null, 0,
            LocalDateTime.now(), "evento-riesgo", "corr-riesgo");

        service.procesarRendimiento(
            new ConsumerRecord<>("siga.academico.rendimiento", 1, 2L, "10", evento));

        var alerta = org.mockito.ArgumentCaptor.forClass(AlertaRiesgo.class);
        verify(publicador).publicar(alerta.capture());
        assertThat(alerta.getValue().getCorrelationId()).isEqualTo("corr-riesgo");
        assertThat(alerta.getValue().getNivelNuevo()).isEqualTo(NivelRiesgo.CRITICO);
    }

    @Test
    void actualizaProyeccionConDatosDelEventoYNivelCriticoSobreCuarenta() {
        EventoReporteProcesadoRepository eventos = mock(EventoReporteProcesadoRepository.class);
        ProyeccionAsistenciaRepository asistencias = mock(ProyeccionAsistenciaRepository.class);
        AlertaRiesgoRepository alertas = mock(AlertaRiesgoRepository.class);
        when(alertas.existsByClaveIdempotencia(any())).thenReturn(false);
        ReporteProcesamientoService service = new ReporteProcesamientoService(
            validator, eventos, asistencias, mock(ProyeccionRendimientoRepository.class), alertas);
        LocalDateTime timestamp = LocalDateTime.of(2026, 11, 9, 22, 0);
        EventoAsistenciaKafka evento = new EventoAsistenciaKafka(
            7L, 10L, 4L, LocalDate.of(2026, 11, 9), State.AUSENTE, null,
            45.0, 20, 9, timestamp, "evento-proyeccion", "corr-proyeccion");

        service.procesarAsistencia(
            new ConsumerRecord<>("siga.academico.asistencias", 0, 3L, "10", evento));

        var proyeccion = org.mockito.ArgumentCaptor.forClass(ProyeccionAsistencia.class);
        verify(asistencias).save(proyeccion.capture());
        ProyeccionAsistencia guardada = proyeccion.getValue();
        assertThat(guardada.getIdEstudiante()).isEqualTo(10L);
        assertThat(guardada.getIdCursoAsignatura()).isEqualTo(4L);
        assertThat(guardada.getIdAsistencia()).isEqualTo(7L);
        assertThat(guardada.getEstado()).isEqualTo(State.AUSENTE);
        assertThat(guardada.getPorcentajeInasistencia()).isEqualTo(45.0);
        assertThat(guardada.getTotalRegistros()).isEqualTo(20L);
        assertThat(guardada.getTotalInasistencias()).isEqualTo(9L);
        assertThat(guardada.getNivelRiesgo()).isEqualTo(NivelRiesgo.CRITICO);
        assertThat(guardada.getIdEvento()).isEqualTo("evento-proyeccion");
        assertThat(guardada.getCorrelationId()).isEqualTo("corr-proyeccion");
        assertThat(guardada.getActualizadoEn()).isEqualTo(timestamp);
    }

    @Test
    void mantieneNivelNormalEnVeintePorCientoYPreventivoSobreVeinte() {
        assertThat(nivelParaPorcentaje(20.0)).isEqualTo(NivelRiesgo.NORMAL);
        assertThat(nivelParaPorcentaje(20.01)).isEqualTo(NivelRiesgo.PREVENTIVO);
    }

    @Test
    void mantieneNivelPreventivoEnCuarentaYCriticoSobreCuarenta() {
        assertThat(nivelParaPorcentaje(40.0)).isEqualTo(NivelRiesgo.PREVENTIVO);
        assertThat(nivelParaPorcentaje(40.01)).isEqualTo(NivelRiesgo.CRITICO);
        assertThat(nivelParaPorcentaje(60.01)).isEqualTo(NivelRiesgo.CRITICO);
    }

    private NivelRiesgo nivelParaPorcentaje(double porcentaje) {
        EventoReporteProcesadoRepository eventos = mock(EventoReporteProcesadoRepository.class);
        ProyeccionAsistenciaRepository asistencias = mock(ProyeccionAsistenciaRepository.class);
        ReporteProcesamientoService service = servicioCon(eventos, asistencias);
        EventoAsistenciaKafka evento = new EventoAsistenciaKafka(
            1L, 10L, 4L, LocalDate.now(), State.AUSENTE, null,
            porcentaje, 100, Math.round(porcentaje), LocalDateTime.now(),
            "evento-" + porcentaje, "corr-" + porcentaje);

        service.procesarAsistencia(
            new ConsumerRecord<>("siga.academico.asistencias", 0, 1L, "10", evento));

        var proyeccion = org.mockito.ArgumentCaptor.forClass(ProyeccionAsistencia.class);
        verify(asistencias).save(proyeccion.capture());
        return proyeccion.getValue().getNivelRiesgo();
    }

    private ReporteProcesamientoService servicioCon(
            EventoReporteProcesadoRepository eventos,
            ProyeccionAsistenciaRepository asistencias) {
        return new ReporteProcesamientoService(
            validator, eventos, asistencias, mock(ProyeccionRendimientoRepository.class),
            mock(AlertaRiesgoRepository.class));
    }

    private ReporteProcesamientoService servicioConPublicador(
            EventoReporteProcesadoRepository eventos,
            ProyeccionAsistenciaRepository asistencias,
            ProyeccionRendimientoRepository rendimiento,
            AlertaRiesgoRepository alertas,
            PublicadorAlertaRiesgo publicador) {
        return new ReporteProcesamientoService(
            validator, eventos, asistencias, rendimiento, alertas, publicador);
    }

    private ReporteProcesamientoService servicio() {
        return new ReporteProcesamientoService(
            validator,
            mock(EventoReporteProcesadoRepository.class),
            mock(ProyeccionAsistenciaRepository.class),
            mock(ProyeccionRendimientoRepository.class),
            mock(AlertaRiesgoRepository.class));
    }
}
