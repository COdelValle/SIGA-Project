package cl.siga.msusuariosauth.service;

import com.rabbitmq.client.Channel;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.usuario.UserRegistrationEventDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStepResultEventDTO;
import cl.siga.coreshare.mensajeria.ConfirmadorMensajes;
import cl.siga.coreshare.messaging.RegistrationCorrelation;
import cl.siga.coreshare.messaging.RegistrationMessagingConstants;
import lombok.RequiredArgsConstructor;

/**
 * Consumidores del orquestador: aprovisionamiento en Entra ID, resultado de la
 * etapa de dominio y sus respectivas DLQ. El correlation id viaja como header
 * AMQP y se publica en el MDC para que los logs sean trazables end-to-end.
 *
 * <p>La confirmación es manual: ACK cuando la etapa termina, NACK hacia la DLQ
 * cuando el evento es inválido o se agotan los reintentos de la causa
 * transitoria.</p>
 */
@Component
@RequiredArgsConstructor
public class AsyncUserRegistrationListeners {

    private final AsyncUserRegistrationService service;
    private final ConfirmadorMensajes confirmador;

    @RabbitListener(queues = RegistrationMessagingConstants.AZURE_QUEUE)
    public void handleAzureSync(
            UserRegistrationEventDTO event,
            @Header(name = RegistrationMessagingConstants.HEADER_CORRELATION_ID, required = false)
            String correlationId,
            Channel canal,
            @Header(AmqpHeaders.DELIVERY_TAG) long etiqueta) {
        confirmador.procesar(canal, etiqueta, RegistrationMessagingConstants.AZURE_QUEUE,
            () -> conCorrelacion(correlationId, event.correlationId(), () -> service.processAzureSync(event)));
    }

    @RabbitListener(queues = RegistrationMessagingConstants.AZURE_DLQ)
    public void handleAzureDlq(UserRegistrationEventDTO event, Channel canal,
            @Header(AmqpHeaders.DELIVERY_TAG) long etiqueta) {
        confirmador.procesar(canal, etiqueta, RegistrationMessagingConstants.AZURE_DLQ,
            () -> conCorrelacion(null, event.correlationId(), () -> service.procesarAzureDlq(event)));
    }

    @RabbitListener(queues = RegistrationMessagingConstants.STATUS_QUEUE)
    public void handleDomainStatus(
            UserRegistrationStepResultEventDTO event,
            @Header(name = RegistrationMessagingConstants.HEADER_CORRELATION_ID, required = false)
            String correlationId,
            Channel canal,
            @Header(AmqpHeaders.DELIVERY_TAG) long etiqueta) {
        confirmador.procesar(canal, etiqueta, RegistrationMessagingConstants.STATUS_QUEUE,
            () -> conCorrelacion(correlationId, null, () -> service.processDomainStepStatus(event)));
    }

    @RabbitListener(queues = RegistrationMessagingConstants.STATUS_DLQ)
    public void handleStatusDlq(UserRegistrationStepResultEventDTO event, Channel canal,
            @Header(AmqpHeaders.DELIVERY_TAG) long etiqueta) {
        confirmador.procesar(canal, etiqueta, RegistrationMessagingConstants.STATUS_DLQ,
            () -> conCorrelacion(null, null, () -> service.procesarStatusDlq(event)));
    }

    private void conCorrelacion(String header, String fallback, Runnable action) {
        String correlationId = (header != null && !header.isBlank()) ? header : fallback;
        RegistrationCorrelation.ejecutar(correlationId, action);
    }
}
