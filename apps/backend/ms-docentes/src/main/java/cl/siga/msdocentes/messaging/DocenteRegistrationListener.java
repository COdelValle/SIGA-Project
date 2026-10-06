package cl.siga.msdocentes.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.docente.RegistrarDocenteRequestDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationEventDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroDocenteDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.messaging.RegistrationCorrelation;
import cl.siga.coreshare.messaging.RegistrationMessagingConstants;
import cl.siga.coreshare.messaging.RegistrationStatusPublisher;
import cl.siga.msdocentes.repository.DocenteRepository;
import cl.siga.msdocentes.service.DocenteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumidor de la etapa de dominio para docentes, idempotente por idUsuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocenteRegistrationListener {

    private final DocenteService docenteService;
    private final DocenteRepository docenteRepository;
    private final RegistrationStatusPublisher statusPublisher;

    @RabbitListener(queues = RegistrationMessagingConstants.DOCENTES_QUEUE)
    public void handle(
            UserRegistrationEventDTO event,
            @Header(name = RegistrationMessagingConstants.HEADER_CORRELATION_ID, required = false)
            String correlationId) {
        String correlacion = (correlationId != null && !correlationId.isBlank())
                ? correlationId
                : event.correlationId();
        RegistrationCorrelation.ejecutar(correlacion, () -> procesar(event));
    }

    @RabbitListener(queues = RegistrationMessagingConstants.DOCENTES_DLQ)
    public void handleDlq(UserRegistrationEventDTO event) {
        try {
            statusPublisher.publicarResultado(event.processId(), event.correlationId(), false,
                    "Se agotaron los reintentos creando el perfil de docente.");
        } catch (RuntimeException ex) {
            log.error("No se pudo reportar el fallo de la cola de docentes del proceso {}: {}",
                    event.processId(), ex.getMessage());
        }
    }

    private void procesar(UserRegistrationEventDTO event) {
        if (!event.schemaSoportado()) {
            fallar(event, "Versión de evento no soportada: " + event.schemaVersion());
            return;
        }
        if (event.requestedRole() != Rol.DOCENTE) {
            fallar(event, "El evento no corresponde al rol DOCENTE.");
            return;
        }
        DatosRegistroDocenteDTO datos = event.roleData() == null ? null : event.roleData().docente();
        if (datos == null || event.userId() == null || event.userId().isBlank()) {
            fallar(event, "El evento de docente está incompleto.");
            return;
        }
        if (docenteRepository.existsByIdUsuario(event.userId())) {
            statusPublisher.publicarResultado(event.processId(), event.correlationId(), true,
                    "El perfil del docente ya existía (idempotente).");
            return;
        }
        try {
            docenteService.saveDocente(new RegistrarDocenteRequestDTO(
                    event.userId(),
                    datos.firstName(),
                    datos.middleName(),
                    datos.firstSurname(),
                    datos.secondSurname(),
                    datos.rut(),
                    datos.fechaContratacion(),
                    datos.area(),
                    datos.certificados()));
            statusPublisher.publicarResultado(event.processId(), event.correlationId(), true,
                    "Perfil de docente creado.");
        } catch (BusinessException ex) {
            statusPublisher.publicarResultado(event.processId(), event.correlationId(), false, ex.getMessage());
        }
    }

    private void fallar(UserRegistrationEventDTO event, String mensaje) {
        log.warn("Evento de registro {} rechazado: {}", event.processId(), mensaje);
        statusPublisher.publicarResultado(event.processId(), event.correlationId(), false, mensaje);
    }
}
