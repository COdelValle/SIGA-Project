package cl.siga.msestudiantes.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.estudiante.RegistrarEstudianteRequestDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationEventDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroEstudianteDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.messaging.RegistrationCorrelation;
import cl.siga.coreshare.messaging.RegistrationMessagingConstants;
import cl.siga.coreshare.messaging.RegistrationStatusPublisher;
import cl.siga.msestudiantes.repository.EstudianteRepository;
import cl.siga.msestudiantes.service.EstudianteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumidor de la etapa de dominio para estudiantes. Es idempotente: si el
 * perfil ya existe (reentrega o reintento posterior a un guardado exitoso) se
 * responde éxito sin volver a crearlo. Errores de negocio se reportan como
 * fallo del proceso; errores transitorios se lanzan para reintentar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EstudianteRegistrationListener {

    private final EstudianteService estudianteService;
    private final EstudianteRepository estudianteRepository;
    private final RegistrationStatusPublisher statusPublisher;

    @RabbitListener(queues = RegistrationMessagingConstants.ESTUDIANTES_QUEUE)
    public void handle(
            UserRegistrationEventDTO event,
            @Header(name = RegistrationMessagingConstants.HEADER_CORRELATION_ID, required = false)
            String correlationId) {
        String correlacion = (correlationId != null && !correlationId.isBlank())
                ? correlationId
                : event.correlationId();
        RegistrationCorrelation.ejecutar(correlacion, () -> procesar(event));
    }

    @RabbitListener(queues = RegistrationMessagingConstants.ESTUDIANTES_DLQ)
    public void handleDlq(UserRegistrationEventDTO event) {
        try {
            statusPublisher.publicarResultado(event.processId(), event.correlationId(), false,
                    "Se agotaron los reintentos creando el perfil de estudiante.");
        } catch (RuntimeException ex) {
            log.error("No se pudo reportar el fallo de la cola de estudiantes del proceso {}: {}",
                    event.processId(), ex.getMessage());
        }
    }

    private void procesar(UserRegistrationEventDTO event) {
        if (!event.schemaSoportado()) {
            fallar(event, "Versión de evento no soportada: " + event.schemaVersion());
            return;
        }
        if (event.requestedRole() != Rol.ESTUDIANTE) {
            fallar(event, "El evento no corresponde al rol ESTUDIANTE.");
            return;
        }
        DatosRegistroEstudianteDTO datos = event.roleData() == null ? null : event.roleData().estudiante();
        if (datos == null || event.userId() == null || event.userId().isBlank()) {
            fallar(event, "El evento de estudiante está incompleto.");
            return;
        }
        if (estudianteRepository.existsByIdUsuario(event.userId())) {
            statusPublisher.publicarResultado(event.processId(), event.correlationId(), true,
                    "El perfil del estudiante ya existía (idempotente).");
            return;
        }
        try {
            estudianteService.saveEstudiante(new RegistrarEstudianteRequestDTO(
                    event.userId(),
                    datos.firstName(),
                    datos.middleName(),
                    datos.firstSurname(),
                    datos.secondSurname(),
                    datos.rut(),
                    datos.birthDate(),
                    datos.allergies(),
                    datos.idClase()));
            statusPublisher.publicarResultado(event.processId(), event.correlationId(), true,
                    "Perfil de estudiante creado.");
        } catch (BusinessException ex) {
            statusPublisher.publicarResultado(event.processId(), event.correlationId(), false, ex.getMessage());
        }
    }

    private void fallar(UserRegistrationEventDTO event, String mensaje) {
        log.warn("Evento de registro {} rechazado: {}", event.processId(), mensaje);
        statusPublisher.publicarResultado(event.processId(), event.correlationId(), false, mensaje);
    }
}
