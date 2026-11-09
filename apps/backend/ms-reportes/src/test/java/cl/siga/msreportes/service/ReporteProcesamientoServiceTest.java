package cl.siga.msreportes.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cl.siga.coreshare.dto.kafka.EventoAsistenciaKafka;
import cl.siga.msreportes.model.entity.AlertaRiesgo;
import cl.siga.msreportes.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private ReporteProcesamientoService servicio() {
        return new ReporteProcesamientoService(
            validator,
            mock(EventoReporteProcesadoRepository.class),
            mock(ProyeccionAsistenciaRepository.class),
            mock(ProyeccionRendimientoRepository.class),
            mock(AlertaRiesgoRepository.class));
    }
}
