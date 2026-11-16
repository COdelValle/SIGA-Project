package cl.siga.msreportes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import cl.siga.msreportes.mensajeria.PublicadorAlertaRiesgo;
import cl.siga.msreportes.repository.AlertaRiesgoRepository;
import cl.siga.msreportes.repository.EventoReporteProcesadoRepository;
import cl.siga.msreportes.repository.ProyeccionAsistenciaRepository;
import cl.siga.msreportes.repository.ProyeccionRendimientoRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class ReporteProcesamientoServiceWiringTest {

    @Test
    void springInyectaElConstructorCompletoAunqueExistaUnaSobrecargaDePrueba() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(Validator.class, () -> mock(Validator.class));
            context.registerBean(EventoReporteProcesadoRepository.class,
                () -> mock(EventoReporteProcesadoRepository.class));
            context.registerBean(ProyeccionAsistenciaRepository.class,
                () -> mock(ProyeccionAsistenciaRepository.class));
            context.registerBean(ProyeccionRendimientoRepository.class,
                () -> mock(ProyeccionRendimientoRepository.class));
            context.registerBean(AlertaRiesgoRepository.class,
                () -> mock(AlertaRiesgoRepository.class));
            context.registerBean(PublicadorAlertaRiesgo.class,
                () -> mock(PublicadorAlertaRiesgo.class));
            context.register(ReporteProcesamientoService.class);

            context.refresh();

            assertThat(context.getBean(ReporteProcesamientoService.class)).isNotNull();
        }
    }
}
