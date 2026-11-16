package cl.siga.msusuariosauth.service;

import com.rabbitmq.client.Channel;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.usuario.UserRegistrationEventDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStepResultEventDTO;
import cl.siga.coreshare.mensajeria.ConfirmadorMensajes;
import cl.siga.coreshare.mensajeria.RegistroMensajeriaProperties;
import cl.siga.coreshare.messaging.MessagingHeaders;
import cl.siga.coreshare.messaging.RegistrationCorrelation;
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
    private final RegistroMensajeriaProperties propiedades;

    @RabbitListener(queues = "${siga.mensajeria.registro.azure-queue}")
    public void handleAzureSync(
            UserRegistrationEventDTO event,
            @Header(name = MessagingHeaders.CORRELATION_ID, required = false)
            String correlationId,
            Channel canal,
            @Header(AmqpHeaders.DELIVERY_TAG) long etiqueta) {
        confirmador.procesar(canal, etiqueta, propiedades.getAzureQueue(),
            () -> conCorrelacion(correlationId, event.correlationId(), () -> service.processAzureSync(event)));
    }

    @RabbitListener(queues = "${siga.mensajeria.registro.azure-dlq}")
    public void handleAzureDlq(UserRegistrationEventDTO event, Channel canal,
            @Header(AmqpHeaders.DELIVERY_TAG) long etiqueta) {
        confirmador.procesar(canal, etiqueta, propiedades.getAzureDlq(),
            () -> conCorrelacion(null, event.correlationId(), () -> service.procesarAzureDlq(event)));
    }

    @RabbitListener(queues = "${siga.mensajeria.registro.status-queue}")
    public void handleDomainStatus(
            UserRegistrationStepResultEventDTO event,
            @Header(name = MessagingHeaders.CORRELATION_ID, required = false)
            String correlationId,
            Channel canal,
            @Header(AmqpHeaders.DELIVERY_TAG) long etiqueta) {
        confirmador.procesar(canal, etiqueta, propiedades.getStatusQueue(),
            () -> conCorrelacion(correlationId, null, () -> service.processDomainStepStatus(event)));
    }

    @RabbitListener(queues = "${siga.mensajeria.registro.status-dlq}")
    public void handleStatusDlq(UserRegistrationStepResultEventDTO event, Channel canal,
            @Header(AmqpHeaders.DELIVERY_TAG) long etiqueta) {
        confirmador.procesar(canal, etiqueta, propiedades.getStatusDlq(),
            () -> conCorrelacion(null, null, () -> service.procesarStatusDlq(event)));
    }

    private void conCorrelacion(String header, String fallback, Runnable action) {
        String correlationId = (header != null && !header.isBlank()) ? header : fallback;
        RegistrationCorrelation.ejecutar(correlationId, action);
    }
}
