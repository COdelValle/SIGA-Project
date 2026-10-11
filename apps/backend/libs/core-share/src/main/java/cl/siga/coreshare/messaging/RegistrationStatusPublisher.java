package cl.siga.coreshare.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import cl.siga.coreshare.dto.usuario.UserRegistrationStepResultEventDTO;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepType;

/**
 * Publica el resultado de la etapa de dominio hacia ms-usuarios-auth con
 * confirmación del broker. Si la confirmación falla lanza una excepción
 * transitoria: el listener revierte/retoma y el reintento vuelve a publicar
 * (los consumidores son idempotentes por {@code idUsuario}).
 */
public class RegistrationStatusPublisher {

    private static final int CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;

    public RegistrationStatusPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarResultado(String processId, String correlationId, boolean success, String message) {
        UserRegistrationStepResultEventDTO evento = new UserRegistrationStepResultEventDTO(
                processId,
                UUID.randomUUID().toString(),
                RegistrationStepType.DOMAIN_SYNC,
                success,
                recortar(message),
                OffsetDateTime.now());

        CorrelationData confirmacion = new CorrelationData(evento.eventId());
        rabbitTemplate.convertAndSend(
                RegistrationMessagingConstants.EXCHANGE,
                RegistrationMessagingConstants.RK_STATUS,
                evento,
                mensaje -> {
                    mensaje.getMessageProperties().setMessageId(evento.eventId());
                    mensaje.getMessageProperties().setContentType("application/json");
                    if (correlationId != null && !correlationId.isBlank()) {
                        mensaje.getMessageProperties().setCorrelationId(correlationId);
                        mensaje.getMessageProperties().setHeader(
                                RegistrationMessagingConstants.HEADER_CORRELATION_ID, correlationId);
                    }
                    return mensaje;
                },
                confirmacion);

        try {
            CorrelationData.Confirm confirm = confirmacion.getFuture()
                    .get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (confirm == null || !confirm.isAck()) {
                throw new IllegalStateException(confirm == null
                        ? "El broker no confirmó el resultado del registro."
                        : "El broker rechazó el resultado del registro: " + confirm.getReason());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrumpido esperando la confirmación del broker.", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Sin confirmación del broker para el resultado del registro.", ex);
        }
    }

    private static String recortar(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return "Sin detalle.";
        }
        return mensaje.length() > 1000 ? mensaje.substring(0, 1000) : mensaje;
    }
}
