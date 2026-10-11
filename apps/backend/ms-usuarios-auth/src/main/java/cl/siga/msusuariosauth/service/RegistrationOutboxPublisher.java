package cl.siga.msusuariosauth.service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.messaging.RegistrationMessagingConstants;
import cl.siga.msusuariosauth.config.RegistroAsyncProperties;
import cl.siga.msusuariosauth.model.entity.OutboxEventState;
import cl.siga.msusuariosauth.model.entity.RegistrationOutboxEvent;
import cl.siga.msusuariosauth.repository.RegistrationOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Publica el outbox del registro asíncrono con confirmación del broker. Si el
 * envío falla, reintenta con backoff exponencial; al agotar
 * {@code outbox-max-attempts} marca el evento FALLIDO y cierra el proceso
 * asociado para que sea visible en el estado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationOutboxPublisher {

    private final RegistrationOutboxEventRepository outboxRepository;
    private final RegistrationStateService stateService;
    private final RabbitTemplate rabbitTemplate;
    private final RegistroAsyncProperties properties;

    @Scheduled(fixedDelayString = "${siga.registro-async.outbox-poll-ms:2000}")
    public void publicarPendientes() {
        if (!properties.isEnabled()) {
            return;
        }
        List<RegistrationOutboxEvent> pendientes = outboxRepository
                .findTop20ByStateAndNextAttemptAtLessThanEqualOrderByIdAsc(
                        OutboxEventState.PENDIENTE, OffsetDateTime.now());
        for (RegistrationOutboxEvent evento : pendientes) {
            publicar(evento);
        }
    }

    private void publicar(RegistrationOutboxEvent evento) {
        try {
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setContentEncoding(StandardCharsets.UTF_8.name());
            props.setMessageId(evento.getEventId());
            if (evento.getCorrelationId() != null) {
                props.setCorrelationId(evento.getCorrelationId());
                props.setHeader(RegistrationMessagingConstants.HEADER_CORRELATION_ID,
                        evento.getCorrelationId());
            }
            Message message = new Message(
                    evento.getPayload().getBytes(StandardCharsets.UTF_8), props);

            CorrelationData confirmacion = new CorrelationData(evento.getEventId());
            rabbitTemplate.send(evento.getExchange(), evento.getRoutingKey(), message, confirmacion);
            CorrelationData.Confirm confirm = confirmacion.getFuture()
                    .get(properties.getOutboxConfirmTimeoutSeconds(), TimeUnit.SECONDS);
            if (confirm == null || !confirm.isAck()) {
                throw new IllegalStateException(confirm == null
                        ? "El broker no confirmó el mensaje."
                        : "El broker rechazó el mensaje: " + confirm.getReason());
            }

            evento.setState(OutboxEventState.ENVIADO);
            evento.setSentAt(OffsetDateTime.now());
            evento.setLastError(null);
            outboxRepository.save(evento);
        } catch (Exception ex) {
            fallo(evento, ex);
        }
    }

    private void fallo(RegistrationOutboxEvent evento, Exception ex) {
        int intentos = evento.getAttempts() + 1;
        evento.setAttempts(intentos);
        String error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        evento.setLastError(recortar(error));
        if (intentos >= properties.getOutboxMaxAttempts()) {
            evento.setState(OutboxEventState.FALLIDO);
            log.error("Outbox {} agotó los reintentos publicando en {} ({}): {}",
                    evento.getEventId(), evento.getExchange(), evento.getRoutingKey(), error);
            stateService.marcarPublicacionFallida(evento.getProcessId(), evento.getRoutingKey(), error);
        } else {
            long segundos = Math.min(60L, 1L << Math.min(intentos, 6));
            evento.setNextAttemptAt(OffsetDateTime.now().plusSeconds(segundos));
            log.warn("Outbox {} falló (intento {}): {}. Reintento en {} s.",
                    evento.getEventId(), intentos, error, segundos);
        }
        outboxRepository.save(evento);
    }

    private static String recortar(String mensaje) {
        return mensaje.length() > 1000 ? mensaje.substring(0, 1000) : mensaje;
    }
}
