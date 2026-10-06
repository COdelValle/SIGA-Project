package cl.siga.msapoderados.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.apoderado.RegistrarApoderadoRequestDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationEventDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroApoderadoDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.messaging.RegistrationCorrelation;
import cl.siga.coreshare.messaging.RegistrationMessagingConstants;
import cl.siga.coreshare.messaging.RegistrationStatusPublisher;
import cl.siga.msapoderados.repository.ApoderadoRepository;
import cl.siga.msapoderados.service.ApoderadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumidor de la etapa de dominio para apoderados, idempotente por idUsuario.
 * La existencia de los pupilos se validó de forma síncrona al aceptar la
 * solicitud (con el token del administrador), por lo que este listener no
 * requiere JWT para el chequeo Feign.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApoderadoRegistrationListener {

    private final ApoderadoService apoderadoService;
    private final ApoderadoRepository apoderadoRepository;
    private final RegistrationStatusPublisher statusPublisher;

    @RabbitListener(queues = RegistrationMessagingConstants.APODERADOS_QUEUE)
    public void handle(
            UserRegistrationEventDTO event,
            @Header(name = RegistrationMessagingConstants.HEADER_CORRELATION_ID, required = false)
            String correlationId) {
        String correlacion = (correlationId != null && !correlationId.isBlank())
                ? correlationId
                : event.correlationId();
        RegistrationCorrelation.ejecutar(correlacion, () -> procesar(event));
    }

    @RabbitListener(queues = RegistrationMessagingConstants.APODERADOS_DLQ)
    public void handleDlq(UserRegistrationEventDTO event) {
        try {
            statusPublisher.publicarResultado(event.processId(), event.correlationId(), false,
                    "Se agotaron los reintentos creando el perfil de apoderado.");
        } catch (RuntimeException ex) {
            log.error("No se pudo reportar el fallo de la cola de apoderados del proceso {}: {}",
                    event.processId(), ex.getMessage());
        }
    }

    private void procesar(UserRegistrationEventDTO event) {
        if (!event.schemaSoportado()) {
            fallar(event, "Versión de evento no soportada: " + event.schemaVersion());
            return;
        }
        if (event.requestedRole() != Rol.APODERADO) {
            fallar(event, "El evento no corresponde al rol APODERADO.");
            return;
        }
        DatosRegistroApoderadoDTO datos = event.roleData() == null ? null : event.roleData().apoderado();
        if (datos == null || event.userId() == null || event.userId().isBlank()) {
            fallar(event, "El evento de apoderado está incompleto.");
            return;
        }
        if (apoderadoRepository.existsByIdUsuario(event.userId())) {
            statusPublisher.publicarResultado(event.processId(), event.correlationId(), true,
                    "El perfil del apoderado ya existía (idempotente).");
            return;
        }
        try {
            apoderadoService.saveApoderadoDesdeEvento(new RegistrarApoderadoRequestDTO(
                    event.userId(),
                    datos.firstName(),
                    datos.middleName(),
                    datos.firstSurname(),
                    datos.secondSurname(),
                    datos.rut(),
                    datos.telefonos(),
                    datos.estudiantes()));
            statusPublisher.publicarResultado(event.processId(), event.correlationId(), true,
                    "Perfil de apoderado creado.");
        } catch (BusinessException ex) {
            statusPublisher.publicarResultado(event.processId(), event.correlationId(), false, ex.getMessage());
        }
    }

    private void fallar(UserRegistrationEventDTO event, String mensaje) {
        log.warn("Evento de registro {} rechazado: {}", event.processId(), mensaje);
        statusPublisher.publicarResultado(event.processId(), event.correlationId(), false, mensaje);
    }
}
