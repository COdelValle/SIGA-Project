package cl.siga.msusuariosauth.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.siga.coreshare.dto.usuario.RegistrarUsuarioCompuestoRequestDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationCredentialResponseDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationEventDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStatusResponseDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStepResultEventDTO;
import cl.siga.coreshare.dto.usuario.enums.RegistrationProcessState;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepState;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepType;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroRolDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ConflictException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.exception.ServiceUnavailableException;
import cl.siga.coreshare.messaging.RegistrationMessagingConstants;
import cl.siga.coreshare.security.SecurityUtils;
import cl.siga.msusuariosauth.config.RegistroAsyncProperties;
import cl.siga.msusuariosauth.integration.estudiantes.EstudianteDirectory;
import cl.siga.msusuariosauth.integration.graph.GraphTransientException;
import cl.siga.msusuariosauth.integration.graph.GraphUserDirectory;
import cl.siga.msusuariosauth.integration.graph.ProvisionedUser;
import cl.siga.msusuariosauth.model.entity.OutboxEventState;
import cl.siga.msusuariosauth.model.entity.RegistrationOutboxEvent;
import cl.siga.msusuariosauth.model.entity.UserRegistrationAttempt;
import cl.siga.msusuariosauth.model.entity.UserRegistrationProcess;
import cl.siga.msusuariosauth.model.entity.Usuario;
import cl.siga.msusuariosauth.repository.ProcessedRegistrationEventRepository;
import cl.siga.msusuariosauth.repository.RegistrationOutboxEventRepository;
import cl.siga.msusuariosauth.repository.UserRegistrationAttemptRepository;
import cl.siga.msusuariosauth.repository.UserRegistrationProcessRepository;
import cl.siga.msusuariosauth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orquestador del registro asíncrono: acepta la solicitud, encola el
 * aprovisionamiento en Entra ID (outbox) y expone estado y credencial. El
 * resultado de cada etapa se persiste en {@link RegistrationStateService} con
 * transacciones independientes para no perder información cuando un reintento
 * revierte la transacción del listener.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncUserRegistrationService {

    private static final List<RegistrationProcessState> ESTADOS_EN_CURSO =
            List.of(RegistrationProcessState.PENDIENTE, RegistrationProcessState.EN_PROCESO);

    private final UserRegistrationProcessRepository processRepository;
    private final UserRegistrationAttemptRepository attemptRepository;
    private final ProcessedRegistrationEventRepository processedEventRepository;
    private final RegistrationOutboxEventRepository outboxRepository;
    private final UsuarioRepository usuarioRepository;
    private final GraphUserDirectory graphDirectory;
    private final EstudianteDirectory estudianteDirectory;
    private final RegistrationStateService stateService;
    private final CredentialCipher credentialCipher;
    private final RegistroAsyncProperties properties;
    private final ObjectMapper objectMapper;
    private final EmailInstitucionalGenerator emailGenerator;

    @Transactional
    public UserRegistrationStatusResponseDTO startRegistration(
            RegistrarUsuarioCompuestoRequestDTO request,
            String correlationIdHeader) {
        if (!properties.isEnabled()) {
            throw new ServiceUnavailableException(
                    "El registro asíncrono de usuarios está deshabilitado.");
        }

        Rol rol = request.requestedRole();
        DatosRegistroRolDTO roleData = request.roleData();
        validarRolYPayload(rol, roleData);

        // Correo y nombre completo: se derivan de los nombres del rol si no vienen.
        String email = isBlank(request.email())
                ? emailGenerator.generarEmail(roleData, rol, this::emailOcupado)
                : normalizeEmail(request.email());
        String fullName = isBlank(request.fullName())
                ? emailGenerator.nombreCompleto(roleData, rol)
                : request.fullName().trim();
        String contactEmail = normalizeNullableEmail(request.contactEmail());
        String oidHint = trimToNull(request.azureUserId());

        // Re-registro de un usuario inactivo (alumno que vuelve): se reutiliza la
        // cuenta y se reactiva en Entra ID. Solo se bloquea si sigue ACTIVO.
        usuarioRepository.findByEmail(email)
                .filter(existente -> existente.getState() == StateUsuario.ACTIVO)
                .ifPresent(existente -> {
                    throw new ConflictException("El correo electrónico ya está registrado y activo.");
                });
        if (processRepository.existsByEmailAndStateIn(email, ESTADOS_EN_CURSO)) {
            throw new ConflictException(
                    "Ya existe un proceso de registro en curso para el correo " + email + ".");
        }
        if (rol == Rol.APODERADO) {
            estudianteDirectory.validarVinculos(request.roleData().apoderado().estudiantes());
        }

        String processId = UUID.randomUUID().toString();
        String correlationId = isBlank(correlationIdHeader) ? processId : correlationIdHeader.trim();

        UserRegistrationEventDTO evento = new UserRegistrationEventDTO(
                UserRegistrationEventDTO.CURRENT_SCHEMA_VERSION,
                UUID.randomUUID().toString(),
                processId,
                correlationId,
                email,
                fullName,
                rol,
                oidHint,
                roleData,
                OffsetDateTime.now());

        UserRegistrationProcess process = UserRegistrationProcess.builder()
                .processId(processId)
                .eventId(evento.eventId())
                .correlationId(correlationId)
                .email(email)
                .fullName(fullName)
                .requestedRole(rol)
                .createdBy(SecurityUtils.getCurrentUserOid().orElse(null))
                .contactEmail(contactEmail)
                .userId(oidHint)
                .roleData(serializarRoleData(request.roleData()))
                .state(RegistrationProcessState.EN_PROCESO)
                .azureState(RegistrationStepState.PENDIENTE)
                .domainState(RegistrationStepState.PENDIENTE)
                .build();
        processRepository.save(process);

        attemptRepository.save(UserRegistrationAttempt.builder()
                .processId(processId)
                .stepType(RegistrationStepType.AZURE_SYNC)
                .success(true)
                .message("Solicitud aceptada; aprovisionamiento en Entra ID en cola.")
                .build());

        outboxRepository.save(RegistrationOutboxEvent.builder()
                .eventId(evento.eventId())
                .processId(processId)
                .exchange(RegistrationMessagingConstants.EXCHANGE)
                .routingKey(RegistrationMessagingConstants.RK_AZURE_SYNC)
                .correlationId(correlationId)
                .payload(serializar(evento))
                .state(OutboxEventState.PENDIENTE)
                .attempts(0)
                .build());

        log.info("Proceso de registro {} aceptado para {} (rol {}).", processId, email, rol);
        return toStatus(process);
    }

    @Transactional(readOnly = true)
    public UserRegistrationStatusResponseDTO getStatus(String processId) {
        return toStatus(findProceso(processId));
    }

    /**
     * Entrega la credencial temporal una única vez, y solo al administrador
     * que inició el proceso.
     */
    @Transactional
    public UserRegistrationCredentialResponseDTO obtenerCredencial(String processId) {
        UserRegistrationProcess process = findProceso(processId);
        String oid = SecurityUtils.getCurrentUserOid().orElse(null);
        if (process.getCreatedBy() != null && !process.getCreatedBy().equals(oid)) {
            throw new AccessDeniedException(
                    "Solo el administrador que inició el proceso puede obtener la credencial.");
        }
        if (process.getAzureState() != RegistrationStepState.COMPLETADO) {
            throw new ConflictException("La cuenta todavía no está aprovisionada en Entra ID.");
        }
        if (process.getCredentialRetrievedAt() != null || process.getCredentialCiphertext() == null) {
            throw new ConflictException(
                    "La credencial ya fue entregada. Usa el restablecimiento de contraseña.");
        }
        if (process.getCredentialExpiresAt() != null
                && process.getCredentialExpiresAt().isBefore(OffsetDateTime.now())) {
            limpiarCredencial(process);
            processRepository.save(process);
            throw new ConflictException(
                    "La credencial temporal expiró. Usa el restablecimiento de contraseña.");
        }

        String password = credentialCipher.descifrar(
                process.getCredentialCiphertext(), process.getCredentialIv());
        process.setCredentialRetrievedAt(OffsetDateTime.now());
        limpiarCredencial(process);
        processRepository.save(process);
        log.info("Credencial del proceso {} entregada al administrador {}.", processId, oid);
        return new UserRegistrationCredentialResponseDTO(
                process.getProcessId(), process.getEmail(), process.getUserId(), password, null);
    }

    /** Restablecimiento síncrono: devuelve la clave nueva directamente (no se persiste). */
    @Transactional
    public UserRegistrationCredentialResponseDTO resetPassword(String idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .filter(u -> u.getState() != cl.siga.coreshare.dto.usuario.enums.StateUsuario.INACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario con ID " + idUsuario + " no encontrado."));
        if (!graphDirectory.isEnabled()) {
            throw new BusinessException(
                    "La integración con Microsoft Graph no está configurada (falta AZURE_CLIENT_SECRET).");
        }
        String password = graphDirectory.resetPassword(usuario.getId());
        log.info("Clave temporal restablecida para el usuario {}.", usuario.getId());
        return new UserRegistrationCredentialResponseDTO(
                null, usuario.getEmail(), usuario.getId(), password, null);
    }

    /** Consumidor de la etapa de Entra ID (evento inicial del outbox). */
    public void processAzureSync(UserRegistrationEventDTO event) {
        if (!event.schemaSoportado()) {
            stateService.fallarAzure(event.processId(), event.eventId(),
                    "Versión de evento no soportada: " + event.schemaVersion(), true);
            return;
        }
        String dedupeKey = "AZURE:" + event.eventId();
        if (processedEventRepository.existsById(dedupeKey)) {
            return;
        }
        UserRegistrationProcess process = findProceso(event.processId());
        if (process.getAzureState() == RegistrationStepState.COMPLETADO
                || process.getState() == RegistrationProcessState.FALLIDO) {
            return;
        }

        try {
            ProvisionedUser provisioned = graphDirectory.provisionUser(
                    event.email(), event.fullName(), event.requestedRole(), event.userId());
            stateService.completarAzure(
                    process.getProcessId(), event.eventId(), provisioned.oid(), provisioned.temporaryPassword());
        } catch (GraphTransientException ex) {
            stateService.registrarIntentoTransitorio(process.getProcessId(), seguro(ex.getMessage()));
            throw ex;
        } catch (BusinessException ex) {
            stateService.fallarAzure(process.getProcessId(), event.eventId(), seguro(ex.getMessage()), true);
        } catch (RuntimeException ex) {
            log.error("Error inesperado aprovisionando el proceso {}", process.getProcessId(), ex);
            stateService.registrarIntentoTransitorio(process.getProcessId(),
                    "Error inesperado durante el aprovisionamiento.");
            throw ex;
        }
    }

    /** La cola de Azure agotó los reintentos: el proceso queda FALLIDO y visible. */
    public void procesarAzureDlq(UserRegistrationEventDTO event) {
        stateService.fallarAzure(event.processId(), event.eventId(),
                "Se agotaron los reintentos de aprovisionamiento en Entra ID.", true);
    }

    /** Resultado de la creación del perfil publicado por el microservicio del rol. */
    public void processDomainStepStatus(UserRegistrationStepResultEventDTO event) {
        stateService.procesarResultadoDominio(event);
    }

    /** La cola de resultados agotó los reintentos: el dominio queda FALLIDO. */
    public void procesarStatusDlq(UserRegistrationStepResultEventDTO event) {
        stateService.fallarDominio(event.processId(), event.eventId(),
                "Se agotaron los reintentos al procesar el resultado de la etapa de dominio.");
    }

    /**
     * Un correo está ocupado si pertenece a un usuario ACTIVO, a un proceso en
     * curso o a una cuenta habilitada en Entra ID. Las cuentas deshabilitadas
     * (soft delete) no cuentan: se reutilizan para reactivar al alumno.
     */
    private boolean emailOcupado(String candidato) {
        boolean activoLocal = usuarioRepository.findByEmail(candidato)
                .filter(usuario -> usuario.getState() == StateUsuario.ACTIVO)
                .isPresent();
        if (activoLocal) {
            return true;
        }
        if (processRepository.existsByEmailAndStateIn(candidato, ESTADOS_EN_CURSO)) {
            return true;
        }
        return graphDirectory.accountEnabledByEmail(candidato).orElse(false);
    }

    private void validarRolYPayload(Rol rol, DatosRegistroRolDTO roleData) {
        if (rol == null || rol == Rol.ADMIN) {
            throw new BusinessException(
                    "El registro asíncrono solo admite los roles ESTUDIANTE, DOCENTE y APODERADO.");
        }
        if (roleData == null) {
            throw new BusinessException("Los datos específicos del rol son obligatorios.");
        }
        boolean estudiante = roleData.estudiante() != null;
        boolean docente = roleData.docente() != null;
        boolean apoderado = roleData.apoderado() != null;
        long informados = (estudiante ? 1 : 0) + (docente ? 1 : 0) + (apoderado ? 1 : 0);
        if (informados != 1) {
            throw new BusinessException(
                    "Debes informar exactamente un bloque de datos de rol (estudiante, docente o apoderado).");
        }
        boolean coincide = switch (rol) {
            case ESTUDIANTE -> estudiante;
            case DOCENTE -> docente;
            case APODERADO -> apoderado;
            case ADMIN -> false;
        };
        if (!coincide) {
            throw new BusinessException(
                    "El bloque de datos no corresponde al rol solicitado (" + rol + ").");
        }
    }

    private void limpiarCredencial(UserRegistrationProcess process) {
        process.setCredentialCiphertext(null);
        process.setCredentialIv(null);
        process.setCredentialExpiresAt(null);
    }

    private UserRegistrationProcess findProceso(String processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proceso de registro " + processId + " no encontrado."));
    }

    private String serializarRoleData(DatosRegistroRolDTO roleData) {
        return serializar(roleData);
    }

    private String serializar(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el evento de registro.", ex);
        }
    }

    private static String seguro(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return "Error de aprovisionamiento en Entra ID.";
        }
        return mensaje.length() > 1000 ? mensaje.substring(0, 1000) : mensaje;
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String normalizeNullableEmail(String email) {
        String normalizado = normalizeEmail(email);
        return normalizado.isEmpty() ? null : normalizado;
    }

    private static String trimToNull(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private UserRegistrationStatusResponseDTO toStatus(UserRegistrationProcess process) {
        return new UserRegistrationStatusResponseDTO(
                process.getProcessId(),
                process.getState(),
                process.getAzureState(),
                process.getDomainState(),
                process.getRequestedRole(),
                process.getEmail(),
                process.getUserId(),
                process.getErrorMessage(),
                process.getCreatedAt(),
                process.getUpdatedAt());
    }
}
