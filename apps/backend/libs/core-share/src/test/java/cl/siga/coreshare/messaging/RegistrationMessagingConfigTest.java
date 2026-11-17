package cl.siga.coreshare.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

import cl.siga.coreshare.mensajeria.ConfiguracionTopologiaNotificaciones;
import cl.siga.coreshare.mensajeria.NotificacionesMensajeriaProperties;
import cl.siga.coreshare.mensajeria.RegistroMensajeriaProperties;

class RegistrationMessagingConfigTest {

    private final RegistroMensajeriaProperties registro = registro();
    private final NotificacionesMensajeriaProperties notificaciones = notificaciones();
    private final RegistrationMessagingConfig config =
        new RegistrationMessagingConfig(registro, notificaciones);

    @Test
    void declaraExchangeTopicColasDurablesYDlq() {
        assertThat(config.userTopicExchange().getName()).isEqualTo(registro.getExchange());
        assertThat(config.userTopicExchange().getType()).isEqualTo("topic");
        assertThat(config.userTopicExchange().isDurable()).isTrue();

        List<Queue> queues = List.of(
            config.userAzureSyncQueue(),
            config.userRegistrationStatusQueue(),
            config.userCredentialsNotifyQueue(),
            config.msEstudiantesQueue(),
            config.msDocentesQueue(),
            config.msApoderadosQueue());
        List<Queue> dlqs = List.of(
            config.userAzureSyncDlq(),
            config.userRegistrationStatusDlq(),
            config.userCredentialsNotifyDlq(),
            config.msEstudiantesDlq(),
            config.msDocentesDlq(),
            config.msApoderadosDlq());

        assertThat(queues).allSatisfy(queue -> assertThat(queue.isDurable()).isTrue());
        assertThat(queues).extracting(Queue::getName).doesNotHaveDuplicates();
        assertThat(dlqs).allSatisfy(queue -> assertThat(queue.isDurable()).isTrue());
        assertThat(dlqs).extracting(Queue::getName).doesNotHaveDuplicates();
        assertThat(queues).allSatisfy(queue ->
            assertThat(queue.getArguments()).containsKey("x-dead-letter-exchange"));
    }

    @Test
    void declaraBindingsDelFlujoPrincipalYDeCadaDlq() {
        var topic = config.userTopicExchange();
        var dlx = new ConfiguracionTopologiaNotificaciones(notificaciones).sigaDeadLetterExchange();
        List<Binding> bindings = List.of(
            config.bindingAzureSync(config.userAzureSyncQueue(), topic),
            config.bindingEstudiantes(config.msEstudiantesQueue(), topic),
            config.bindingDocentes(config.msDocentesQueue(), topic),
            config.bindingApoderados(config.msApoderadosQueue(), topic),
            config.bindingStatus(config.userRegistrationStatusQueue(), topic),
            config.bindingCredentialsNotify(config.userCredentialsNotifyQueue(), topic),
            config.bindingAzureSyncDlq(config.userAzureSyncDlq(), dlx),
            config.bindingEstudiantesDlq(config.msEstudiantesDlq(), dlx),
            config.bindingDocentesDlq(config.msDocentesDlq(), dlx),
            config.bindingApoderadosDlq(config.msApoderadosDlq(), dlx),
            config.bindingStatusDlq(config.userRegistrationStatusDlq(), dlx),
            config.bindingCredentialsNotifyDlq(config.userCredentialsNotifyDlq(), dlx));

        assertThat(bindings).hasSize(12);
        assertThat(bindings).allSatisfy(binding -> assertThat(binding.getDestination()).isNotBlank());
        assertThat(bindings.stream()
            .filter(binding -> binding.getExchange().equals(notificaciones.getExchangeDlq())))
            .hasSize(6);
    }

    @Test
    void losNombresDelRegistroSePuedenCambiarSinModificarJava() {
        RegistroMensajeriaProperties personalizadas = registro();
        personalizadas.setEstudiantesQueue("cola-estudiantes-personalizada");
        personalizadas.setEstudiantesDlq("cola-estudiantes-personalizada.dlq");

        RegistrationMessagingConfig otra =
            new RegistrationMessagingConfig(personalizadas, notificaciones);

        assertThat(otra.msEstudiantesQueue().getName())
            .isEqualTo("cola-estudiantes-personalizada");
        assertThat(otra.bindingEstudiantes(otra.msEstudiantesQueue(), otra.userTopicExchange())
            .getDestination()).isEqualTo("cola-estudiantes-personalizada");
    }

    static RegistroMensajeriaProperties registro() {
        RegistroMensajeriaProperties p = new RegistroMensajeriaProperties();
        p.setExchange("user.topic.exchange");
        p.setAzureQueue("user.azure.sync.queue");
        p.setEstudiantesQueue("ms.estudiantes.queue");
        p.setDocentesQueue("ms.docentes.queue");
        p.setApoderadosQueue("ms.apoderados.queue");
        p.setStatusQueue("user.registration.status.queue");
        p.setCredentialsQueue("user.credentials.notify.queue");
        p.setAzureDlq("user.azure.sync.dlq");
        p.setEstudiantesDlq("ms.estudiantes.dlq");
        p.setDocentesDlq("ms.docentes.dlq");
        p.setApoderadosDlq("ms.apoderados.dlq");
        p.setStatusDlq("user.registration.status.dlq");
        p.setCredentialsDlq("user.credentials.notify.dlq");
        p.setRkAzureSync("user.registration.azure");
        p.setRkRegisterEstudiante("user.register.estudiante");
        p.setRkRegisterDocente("user.register.docente");
        p.setRkRegisterApoderado("user.register.apoderado");
        p.setRkStatus("user.registration.status");
        p.setRkCredentialsNotify("user.credentials.notify");
        p.setHeaderCorrelationId("X-Correlation-Id");
        return p;
    }

    static NotificacionesMensajeriaProperties notificaciones() {
        NotificacionesMensajeriaProperties p = new NotificacionesMensajeriaProperties();
        p.setExchangeNotificaciones("intercambio-notificaciones");
        p.setExchangeDlq("siga.dlx.direct");
        p.setColaEvaluaciones("cola-notificaciones-evaluaciones");
        p.setDlqEvaluaciones("cola-notificaciones-evaluaciones.dlq");
        p.setColaAsistencias("cola-notificaciones-asistencias");
        p.setDlqAsistencias("cola-notificaciones-asistencias.dlq");
        p.setColaNotas("cola-notificaciones-notas");
        p.setDlqNotas("cola-notificaciones-notas.dlq");
        p.setColaRiesgoInasistencia("cola-notificaciones-riesgo-inasistencia");
        p.setDlqRiesgoInasistencia("cola-notificaciones-riesgo-inasistencia.dlq");
        p.setColaRiesgoRendimiento("cola-notificaciones-riesgo-rendimiento");
        p.setDlqRiesgoRendimiento("cola-notificaciones-riesgo-rendimiento.dlq");
        p.setColaCambioEstadoAcademico("cola-notificaciones-cambio-estado");
        p.setDlqCambioEstadoAcademico("cola-notificaciones-cambio-estado.dlq");
        p.setPatronEvaluacion("evaluacion.*");
        p.setPatronAsistencia("asistencia.*");
        p.setPatronNota("nota.*");
        p.setClaveEvaluacionCreada("evaluacion.creada");
        p.setClaveEvaluacionActualizada("evaluacion.actualizada");
        p.setClaveEvaluacionEliminada("evaluacion.eliminada");
        p.setClaveAsistenciaRegistrada("asistencia.registrada");
        p.setClaveRiesgoInasistencia("riesgo.inasistencia");
        p.setClaveRiesgoRendimiento("riesgo.rendimiento");
        p.setClaveCambioEstadoAcademico("riesgo.estado-academico");
        p.setClaveNotaCreada("nota.creada");
        p.setClaveNotaActualizada("nota.actualizada");
        return p;
    }
}
