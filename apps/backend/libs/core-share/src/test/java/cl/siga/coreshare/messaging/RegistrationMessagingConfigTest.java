package cl.siga.coreshare.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

import cl.siga.coreshare.mensajeria.ConfiguracionTopologiaNotificaciones;
import cl.siga.coreshare.mensajeria.NombresMensajeria;

class RegistrationMessagingConfigTest {

    private final RegistrationMessagingConfig config = new RegistrationMessagingConfig();

    @Test
    void declaraExchangeTopicColasDurablesYDlq() {
        assertThat(config.userTopicExchange().getName()).isEqualTo(RegistrationMessagingConstants.EXCHANGE);
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
        var dlx = new ConfiguracionTopologiaNotificaciones().sigaDeadLetterExchange();
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
            .filter(binding -> binding.getExchange().equals(NombresMensajeria.INTERCAMBIO_DLQ)))
            .hasSize(6);
    }
}
