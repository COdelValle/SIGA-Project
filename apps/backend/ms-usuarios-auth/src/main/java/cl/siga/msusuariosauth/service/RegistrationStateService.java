package cl.siga.msusuariosauth.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.siga.coreshare.dto.usuario.UserCredentialsNotificationEventDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationEventDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStepResultEventDTO;
import cl.siga.coreshare.dto.usuario.enums.RegistrationProcessState;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepState;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepType;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateInvitacion;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroRolDTO;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.messaging.RegistrationMessagingConstants;
import cl.siga.msusuariosauth.config.RegistroAsyncProperties;
import cl.siga.msusuariosauth.model.entity.OutboxEventState;
import cl.siga.msusuariosauth.model.entity.ProcessedRegistrationEvent;
import cl.siga.msusuariosauth.model.entity.RegistrationOutboxEvent;
import cl.siga.msusuariosauth.model.entity.UserRegistrationAttempt;
import cl.siga.msusuariosauth.model.entity.UserRegistrationProcess;
import cl.siga.msusuariosauth.model.entity.Usuario;
import cl.siga.msusuariosauth.repository.InvitacionUsuarioRepository;
import cl.siga.msusuariosauth.repository.ProcessedRegistrationEventRepository;
import cl.siga.msusuariosauth.repository.RegistrationOutboxEventRepository;
import cl.siga.msusuariosauth.repository.UserRegistrationAttemptRepository;
import cl.siga.msusuariosauth.repository.UserRegistrationProcessRepository;
import cl.siga.msusuariosauth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Persistencia de los cambios de estado del registro asíncrono. Cada método
 * corre en una transacción independiente ({@code REQUIRES_NEW}) para que el
 * resultado de un fallo de Graph no se pierda cuando la transacción del
 * listener se revierte por el reintento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationStateService {

    private final UserRegistrationProcessRepository processRepository;
    private final UserRegistrationAttemptRepository attemptRepository;
    private final ProcessedRegistrationEventRepository processedEventRepository;
    private final RegistrationOutboxEventRepository outboxRepository;
    private final UsuarioRepository usuarioRepository;
    private final InvitacionUsuarioRepository invitacionRepository;
    private final CredentialCipher credentialCipher;
    private final RegistroAsyncProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Éxito del aprovisionamiento: crea/activa el usuario local, vincula la
     * invitación previa, cifra la credencial temporal, encola el evento de
     * dominio (y el de notificación si está habilitado) y cierra la etapa.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completarAzure(String processId, String eventId, String oid, String temporaryPassword) {
        UserRegistrationProcess process = findProceso(processId);
        if (process.getAzureState() == RegistrationStepState.COMPLETADO) {
            return;
        }

        Usuario usuario = usuarioRepository.findById(oid)
                .orElseGet(() -> Usuario.builder().id(oid).build());
        usuario.setEmail(process.getEmail());
        if (process.getFullName() != null && !process.getFullName().isBlank()) {
            usuario.setFullName(process.getFullName());
        }
        usuario.setRol(process.getRequestedRole());
        usuario.setState(StateUsuario.ACTIVO);
        usuarioRepository.save(usuario);

        invitacionRepository.findByEmail(process.getEmail())
                .filter(invitacion -> invitacion.getState() == StateInvitacion.INVITADO)
                .ifPresent(invitacion -> {
                    invitacion.setState(StateInvitacion.VINCULADA);
                    invitacion.setBoundAt(OffsetDateTime.now().toLocalDateTime());
                    invitacionRepository.save(invitacion);
                });

        CredentialCipher.Cifrado cifrado = credentialCipher.cifrar(temporaryPassword);
        process.setCredentialCiphertext(cifrado.ciphertext());
        process.setCredentialIv(cifrado.iv());
        process.setCredentialExpiresAt(OffsetDateTime.now().plusHours(properties.getCredentialTtlHours()));
        process.setCredentialRetrievedAt(null);
        process.setUserId(oid);
        process.setAzureState(RegistrationStepState.COMPLETADO);
        process.setErrorMessage(null);
        recalcularEstado(process);
        processRepository.save(process);

        registrarIntento(processId, RegistrationStepType.AZURE_SYNC, true,
                "Cuenta aprovisionada en Entra ID y rol asignado.");
        encolarEventoDominio(process, oid);
        if (properties.isNotifyCredentialsEnabled()) {
            encolarAvisoCredenciales(process, oid);
        }
        marcarEventoProcesado("AZURE:" + eventId);
        log.info("Proceso {} completó la etapa de Entra ID (oid {}).", processId, oid);
    }

    /**
     * Fallo definitivo de la etapa de Entra ID. Persiste el estado aunque la
     * transacción del listener se revierta.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fallarAzure(String processId, String eventId, String mensaje, boolean marcarDedupe) {
        UserRegistrationProcess process = findProceso(processId);
        if (process.getAzureState() == RegistrationStepState.COMPLETADO) {
            return;
        }
        process.setAzureState(RegistrationStepState.FALLIDO);
        process.setErrorMessage(recortar(mensaje));
        recalcularEstado(process);
        processRepository.save(process);
        registrarIntento(processId, RegistrationStepType.AZURE_SYNC, false, mensaje);
        if (marcarDedupe) {
            marcarEventoProcesado("AZURE:" + eventId);
        }
    }

    /** Registra un intento fallido transitorio sin cerrar el proceso (habrá reintento). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIntentoTransitorio(String processId, String mensaje) {
        registrarIntento(processId, RegistrationStepType.AZURE_SYNC, false, mensaje);
    }

    /** Resultado de la creación del perfil publicada por el microservicio del rol. */
    @Transactional
    public void procesarResultadoDominio(UserRegistrationStepResultEventDTO event) {
        String dedupeKey = "DOMAIN:" + event.eventId();
        if (processedEventRepository.existsById(dedupeKey)) {
            return;
        }
        UserRegistrationProcess process = findProceso(event.processId());
        process.setDomainState(event.success()
                ? RegistrationStepState.COMPLETADO
                : RegistrationStepState.FALLIDO);
        if (event.success()) {
            process.setErrorMessage(null);
        } else {
            process.setErrorMessage(recortar(event.message()));
        }
        recalcularEstado(process);
        processRepository.save(process);
        registrarIntento(process.getProcessId(),
                event.stepType() == null ? RegistrationStepType.DOMAIN_SYNC : event.stepType(),
                event.success(), event.message());
        marcarEventoProcesado(dedupeKey);
    }

    /** La cola de resultados de dominio agotó los reintentos. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fallarDominio(String processId, String eventId, String mensaje) {
        UserRegistrationProcess process = findProceso(processId);
        if (process.getDomainState() == RegistrationStepState.COMPLETADO) {
            return;
        }
        process.setDomainState(RegistrationStepState.FALLIDO);
        process.setErrorMessage(recortar(mensaje));
        recalcularEstado(process);
        processRepository.save(process);
        registrarIntento(processId, RegistrationStepType.DOMAIN_SYNC, false, mensaje);
        marcarEventoProcesado("DOMAIN:" + eventId);
    }

    /** La publicación de un evento se agotó: si el proceso no terminó, se marca FALLIDO. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarPublicacionFallida(String processId, String routingKey, String error) {
        if (processId == null) {
            return;
        }
        processRepository.findById(processId).ifPresent(process -> {
            if (process.getState() == RegistrationProcessState.COMPLETADO) {
                return;
            }
            process.setState(RegistrationProcessState.FALLIDO);
            process.setErrorMessage(recortar(
                    "No se pudo publicar el evento " + routingKey + ": " + error));
            processRepository.save(process);
        });
    }

    private void encolarEventoDominio(UserRegistrationProcess process, String oid) {
        DatosRegistroRolDTO roleData = leerRoleData(process);
        UserRegistrationEventDTO evento = new UserRegistrationEventDTO(
                UserRegistrationEventDTO.CURRENT_SCHEMA_VERSION,
                UUID.randomUUID().toString(),
                process.getProcessId(),
                process.getCorrelationId(),
                process.getEmail(),
                process.getFullName(),
                process.getRequestedRole(),
                oid,
                roleData,
                OffsetDateTime.now());
        encolar(evento.eventId(), process.getProcessId(), process.getCorrelationId(),
                routingKeyPorRol(process.getRequestedRole()), serializar(evento));
    }

    private void encolarAvisoCredenciales(UserRegistrationProcess process, String oid) {
        UserCredentialsNotificationEventDTO evento = new UserCredentialsNotificationEventDTO(
                UserCredentialsNotificationEventDTO.CURRENT_SCHEMA_VERSION,
                UUID.randomUUID().toString(),
                process.getProcessId(),
                process.getCorrelationId(),
                process.getEmail(),
                process.getContactEmail(),
                oid,
                process.getRequestedRole(),
                OffsetDateTime.now());
        encolar(evento.eventId(), process.getProcessId(), process.getCorrelationId(),
                RegistrationMessagingConstants.RK_CREDENTIALS_NOTIFY, serializar(evento));
    }

    private void encolar(String eventId, String processId, String correlationId,
            String routingKey, String payload) {
        outboxRepository.save(RegistrationOutboxEvent.builder()
                .eventId(eventId)
                .processId(processId)
                .exchange(RegistrationMessagingConstants.EXCHANGE)
                .routingKey(routingKey)
                .correlationId(correlationId)
                .payload(payload)
                .state(OutboxEventState.PENDIENTE)
                .attempts(0)
                .build());
    }

    private String routingKeyPorRol(Rol rol) {
        return switch (rol) {
            case ESTUDIANTE -> RegistrationMessagingConstants.RK_REGISTER_ESTUDIANTE;
            case DOCENTE -> RegistrationMessagingConstants.RK_REGISTER_DOCENTE;
            case APODERADO -> RegistrationMessagingConstants.RK_REGISTER_APODERADO;
            case ADMIN -> throw new IllegalStateException("El rol ADMIN no usa el registro asíncrono.");
        };
    }

    private DatosRegistroRolDTO leerRoleData(UserRegistrationProcess process) {
        try {
            return objectMapper.readValue(process.getRoleData(), DatosRegistroRolDTO.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "No se pudo reconstruir el payload del rol del proceso " + process.getProcessId(), ex);
        }
    }

    private String serializar(Object evento) {
        try {
            return objectMapper.writeValueAsString(evento);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el evento de registro.", ex);
        }
    }

    private void recalcularEstado(UserRegistrationProcess process) {
        if (process.getAzureState() == RegistrationStepState.FALLIDO
                || process.getDomainState() == RegistrationStepState.FALLIDO) {
            process.setState(RegistrationProcessState.FALLIDO);
            return;
        }
        if (process.getAzureState() == RegistrationStepState.COMPLETADO
                && process.getDomainState() == RegistrationStepState.COMPLETADO) {
            process.setState(RegistrationProcessState.COMPLETADO);
        } else {
            process.setState(RegistrationProcessState.EN_PROCESO);
        }
    }

    private void registrarIntento(String processId, RegistrationStepType tipo, boolean exito, String mensaje) {
        attemptRepository.save(UserRegistrationAttempt.builder()
                .processId(processId)
                .stepType(tipo)
                .success(exito)
                .message(recortar(mensaje))
                .build());
    }

    private void marcarEventoProcesado(String dedupeKey) {
        processedEventRepository.save(ProcessedRegistrationEvent.builder().eventId(dedupeKey).build());
    }

    private UserRegistrationProcess findProceso(String processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proceso de registro " + processId + " no encontrado."));
    }

    private static String recortar(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return "Sin detalle.";
        }
        return mensaje.length() > 1000 ? mensaje.substring(0, 1000) : mensaje;
    }
}
