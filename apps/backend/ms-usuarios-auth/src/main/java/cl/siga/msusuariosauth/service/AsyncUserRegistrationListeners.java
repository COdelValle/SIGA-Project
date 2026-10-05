package cl.siga.msusuariosauth.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.usuario.UserRegistrationEventDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStepResultEventDTO;
import cl.siga.coreshare.messaging.RegistrationMessagingConstants;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AsyncUserRegistrationListeners {

    private final AsyncUserRegistrationService service;

    @RabbitListener(queues = RegistrationMessagingConstants.AZURE_QUEUE)
    public void handleAzureSync(UserRegistrationEventDTO event) {
        service.processAzureSync(event);
    }

    @RabbitListener(queues = RegistrationMessagingConstants.STATUS_QUEUE)
    public void handleDomainStatus(UserRegistrationStepResultEventDTO event) {
        service.processDomainStepStatus(event);
    }
}
