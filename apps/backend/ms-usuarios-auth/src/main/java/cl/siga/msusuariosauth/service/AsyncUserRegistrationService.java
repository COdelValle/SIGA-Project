package cl.siga.msusuariosauth.service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cl.siga.coreshare.dto.usuario.RegistrarUsuarioCompuestoRequestDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationEventDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStatusResponseDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStepResultEventDTO;
import cl.siga.coreshare.dto.usuario.enums.RegistrationProcessState;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepState;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepType;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.messaging.RegistrationMessagingConstants;
import cl.siga.msusuariosauth.integration.graph.GraphUserDirectory;
import cl.siga.msusuariosauth.model.entity.ProcessedRegistrationEvent;
import cl.siga.msusuariosauth.model.entity.UserRegistrationAttempt;
import cl.siga.msusuariosauth.model.entity.UserRegistrationProcess;
import cl.siga.msusuariosauth.model.entity.Usuario;
import cl.siga.msusuariosauth.repository.ProcessedRegistrationEventRepository;
import cl.siga.msusuariosauth.repository.UserRegistrationAttemptRepository;
import cl.siga.msusuariosauth.repository.UserRegistrationProcessRepository;
import cl.siga.msusuariosauth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncUserRegistrationService {

    private final UserRegistrationProcessRepository processRepository;
    private final UserRegistrationAttemptRepository attemptRepository;
    private final ProcessedRegistrationEventRepository processedEventRepository;
    private final UsuarioRepository usuarioRepository;
    private final GraphUserDirectory graphDirectory;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public UserRegistrationStatusResponseDTO startRegistration(
            RegistrarUsuarioCompuestoRequestDTO request,
            String correlationIdHeader) {
        String email = normalizeEmail(request.email());
        String userId = resolveUserId(request, email);

        if (usuarioRepository.existsById(userId)) {
            throw new BusinessException("El usuario con ID de Azure " + userId + " ya está registrado.");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new BusinessException("El correo electrónico ya está registrado.");
        }

        Usuario baseUsuario = Usuario.builder()
                .id(userId)
                .email(email)
                .rol(request.requestedRole())
                .state(cl.siga.coreshare.dto.usuario.enums.StateUsuario.INVITADO)
                .build();
        usuarioRepository.save(baseUsuario);

        String processId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        String correlationId = isBlank(correlationIdHeader) ? processId : correlationIdHeader.trim();

        UserRegistrationProcess process = UserRegistrationProcess.builder()
                .processId(processId)
                .eventId(eventId)
                .correlationId(correlationId)
                .email(email)
                .fullName(request.fullName().trim())
                .requestedRole(request.requestedRole())
                .userId(userId)
                .state(RegistrationProcessState.PENDIENTE)
                .azureState(RegistrationStepState.PENDIENTE)
                .domainState(request.requestedRole() == Rol.ADMIN
                        ? RegistrationStepState.COMPLETADO
                        : RegistrationStepState.PENDIENTE)
                .build();
        processRepository.save(process);

        attemptRepository.save(UserRegistrationAttempt.builder()
                .processId(processId)
                .stepType(RegistrationStepType.DOMAIN_SYNC)
                .success(true)
                .message("Solicitud aceptada y publicada en cola.")
                .build());

        UserRegistrationEventDTO event = new UserRegistrationEventDTO(
                "v1",
                eventId,
                processId,
                correlationId,
                email,
                request.fullName().trim(),
                request.requestedRole(),
                userId,
                request.roleData(),
                OffsetDateTime.now());
        String routingKey = routingKeyFor(request.requestedRole());
        runAfterCommit(() -> rabbitTemplate.convertAndSend(
                RegistrationMessagingConstants.EXCHANGE,
                routingKey,
                event));

        process.setState(RegistrationProcessState.EN_PROCESO);
        processRepository.save(process);
        return toStatus(process);
    }

    @Transactional(readOnly = true)
    public UserRegistrationStatusResponseDTO getStatus(String processId) {
        UserRegistrationProcess process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Proceso de registro " + processId + " no encontrado."));
        return toStatus(process);
    }

    @Transactional
    public void processAzureSync(UserRegistrationEventDTO event) {
        String dedupeKey = "AZURE:" + event.eventId();
        if (processedEventRepository.existsById(dedupeKey)) {
            return;
        }

        UserRegistrationProcess process = findProcess(event.processId());
        try {
            if (!graphDirectory.isEnabled()) {
                throw new BusinessException("No se puede sincronizar roles en Graph: integración deshabilitada.");
            }
            graphDirectory.syncRole(event.userId(), event.requestedRole());

            process.setAzureState(RegistrationStepState.COMPLETADO);
            Usuario usuario = usuarioRepository.findById(event.userId()).orElse(null);
            if (usuario != null) {
                usuario.setRol(event.requestedRole());
                usuario.setState(cl.siga.coreshare.dto.usuario.enums.StateUsuario.ACTIVO);
                usuarioRepository.save(usuario);
            }
            markOverallState(process);
            attemptRepository.save(UserRegistrationAttempt.builder()
                    .processId(process.getProcessId())
                    .stepType(RegistrationStepType.AZURE_SYNC)
                    .success(true)
                    .message("Sincronización de rol en Entra ID completada.")
                    .build());
        } catch (RuntimeException ex) {
            process.setAzureState(RegistrationStepState.FALLIDO);
            process.setState(RegistrationProcessState.FALLIDO);
            process.setErrorMessage(ex.getMessage());
            attemptRepository.save(UserRegistrationAttempt.builder()
                    .processId(process.getProcessId())
                    .stepType(RegistrationStepType.AZURE_SYNC)
                    .success(false)
                    .message(ex.getMessage())
                    .build());
            throw ex;
        } finally {
            processRepository.save(process);
            processedEventRepository.save(ProcessedRegistrationEvent.builder().eventId(dedupeKey).build());
        }
    }

    @Transactional
    public void processDomainStepStatus(UserRegistrationStepResultEventDTO event) {
        String dedupeKey = "DOMAIN:" + event.eventId();
        if (processedEventRepository.existsById(dedupeKey)) {
            return;
        }
        UserRegistrationProcess process = findProcess(event.processId());

        process.setDomainState(event.success() ? RegistrationStepState.COMPLETADO : RegistrationStepState.FALLIDO);
        if (!event.success()) {
            process.setErrorMessage(event.message());
            process.setState(RegistrationProcessState.FALLIDO);
        } else {
            markOverallState(process);
        }

        attemptRepository.save(UserRegistrationAttempt.builder()
                .processId(process.getProcessId())
                .stepType(RegistrationStepType.DOMAIN_SYNC)
                .success(event.success())
                .message(event.message())
                .build());
        processRepository.save(process);
        processedEventRepository.save(ProcessedRegistrationEvent.builder().eventId(dedupeKey).build());
    }

    private void markOverallState(UserRegistrationProcess process) {
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

    private String resolveUserId(RegistrarUsuarioCompuestoRequestDTO request, String email) {
        if (!isBlank(request.azureUserId())) {
            return request.azureUserId().trim();
        }
        if (graphDirectory.isEnabled()) {
            Optional<String> oid = graphDirectory.findUserByEmail(email).map(u -> u.oid());
            if (oid.isPresent()) {
                return oid.get();
            }
        }
        throw new BusinessException(
                "Debe enviar azureUserId o usar un correo existente en Entra ID para iniciar el registro asíncrono.");
    }

    private String routingKeyFor(Rol role) {
        return switch (role) {
            case ESTUDIANTE -> RegistrationMessagingConstants.RK_REGISTER_ESTUDIANTE;
            case DOCENTE -> RegistrationMessagingConstants.RK_REGISTER_DOCENTE;
            case APODERADO -> RegistrationMessagingConstants.RK_REGISTER_APODERADO;
            case ADMIN -> RegistrationMessagingConstants.RK_REGISTER_ADMIN;
        };
    }

    private UserRegistrationProcess findProcess(String processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proceso de registro " + processId + " no encontrado."));
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
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

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
