package cl.siga.msusuariosauth.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.usuario.UserRegistrationEventDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStepResultEventDTO;
import cl.siga.coreshare.messaging.RegistrationCorrelation;
import cl.siga.coreshare.messaging.RegistrationMessagingConstants;
import lombok.RequiredArgsConstructor;

/**
 * Consumidores del orquestador: aprovisionamiento en Entra ID, resultado de la
 * etapa de dominio y sus respectivas DLQ. El correlation id viaja como header
 * AMQP y se publica en el MDC para que los logs sean trazables end-to-end.
 */
@Component
@RequiredArgsConstructor
public class AsyncUserRegistrationListeners {

    private final AsyncUserRegistrationService service;

    @RabbitListener(queues = RegistrationMessagingConstants.AZURE_QUEUE)
    public void handleAzureSync(
            UserRegistrationEventDTO event,
            @Header(name = RegistrationMessagingConstants.HEADER_CORRELATION_ID, required = false)
            String correlationId) {
        conCorrelacion(correlationId, event.correlationId(), () -> service.processAzureSync(event));
    }

    @RabbitListener(queues = RegistrationMessagingConstants.AZURE_DLQ)
    public void handleAzureDlq(UserRegistrationEventDTO event) {
        conCorrelacion(null, event.correlationId(), () -> service.procesarAzureDlq(event));
    }

    @RabbitListener(queues = RegistrationMessagingConstants.STATUS_QUEUE)
    public void handleDomainStatus(
            UserRegistrationStepResultEventDTO event,
            @Header(name = RegistrationMessagingConstants.HEADER_CORRELATION_ID, required = false)
            String correlationId) {
        conCorrelacion(correlationId, null, () -> service.processDomainStepStatus(event));
    }

    @RabbitListener(queues = RegistrationMessagingConstants.STATUS_DLQ)
    public void handleStatusDlq(UserRegistrationStepResultEventDTO event) {
        conCorrelacion(null, null, () -> service.procesarStatusDlq(event));
    }

    private void conCorrelacion(String header, String fallback, Runnable action) {
        String correlationId = (header != null && !header.isBlank()) ? header : fallback;
        RegistrationCorrelation.ejecutar(correlationId, action);
    }
}
