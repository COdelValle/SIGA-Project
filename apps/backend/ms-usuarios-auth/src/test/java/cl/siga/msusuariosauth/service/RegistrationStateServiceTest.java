package cl.siga.msusuariosauth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cl.siga.coreshare.dto.usuario.UserRegistrationStepResultEventDTO;
import cl.siga.coreshare.dto.usuario.enums.RegistrationProcessState;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepState;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepType;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateInvitacion;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.messaging.RegistrationMessagingConstants;
import cl.siga.msusuariosauth.config.RegistroAsyncProperties;
import cl.siga.msusuariosauth.model.entity.InvitacionUsuario;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationStateServiceTest {

    private static final String PROCESS_ID = "process-1";
    private static final String EVENT_ID = "event-1";
    private static final String OID = "oid-123";
    private static final String EMAIL = "nuevo@test.com";

    @Mock
    private UserRegistrationProcessRepository processRepository;
    @Mock
    private UserRegistrationAttemptRepository attemptRepository;
    @Mock
    private ProcessedRegistrationEventRepository processedEventRepository;
    @Mock
    private RegistrationOutboxEventRepository outboxRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private InvitacionUsuarioRepository invitacionRepository;
    @Mock
    private CredentialCipher credentialCipher;

    private final RegistroAsyncProperties properties = new RegistroAsyncProperties();
    private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    private RegistrationStateService service;

    @BeforeEach
    void setUp() {
        service = new RegistrationStateService(
                processRepository,
                attemptRepository,
                processedEventRepository,
                outboxRepository,
                usuarioRepository,
                invitacionRepository,
                credentialCipher,
                properties,
                objectMapper);
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private UserRegistrationProcess proceso() {
        return UserRegistrationProcess.builder()
                .processId(PROCESS_ID)
                .eventId(EVENT_ID)
                .correlationId("corr-1")
                .email(EMAIL)
                .fullName("Ana Perez")
                .requestedRole(Rol.ESTUDIANTE)
                .contactEmail("contacto@test.com")
                .roleData("{}")
                .state(RegistrationProcessState.EN_PROCESO)
                .azureState(RegistrationStepState.PENDIENTE)
                .domainState(RegistrationStepState.PENDIENTE)
                .build();
    }

    private void stubCompletarAzure(UserRegistrationProcess process) {
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(usuarioRepository.findById(OID)).thenReturn(Optional.empty());
        when(invitacionRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(credentialCipher.cifrar("Temp-123")).thenReturn(new CredentialCipher.Cifrado("cifrado", "iv"));
    }

    @Test
    void completarAzureCreaUsuarioYEncolaEventoDeDominio() {
        UserRegistrationProcess process = proceso();
        stubCompletarAzure(process);

        service.completarAzure(PROCESS_ID, EVENT_ID, OID, "Temp-123");

        assertEquals(RegistrationStepState.COMPLETADO, process.getAzureState());
        assertEquals(RegistrationProcessState.EN_PROCESO, process.getState());
        assertEquals(OID, process.getUserId());
        assertEquals("cifrado", process.getCredentialCiphertext());
        assertEquals("iv", process.getCredentialIv());
        assertNotNull(process.getCredentialExpiresAt());
        assertNull(process.getCredentialRetrievedAt());
        assertNull(process.getErrorMessage());

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuario.capture());
        assertEquals(OID, usuario.getValue().getId());
        assertEquals(EMAIL, usuario.getValue().getEmail());
        assertEquals("Ana Perez", usuario.getValue().getFullName());
        assertEquals(Rol.ESTUDIANTE, usuario.getValue().getRol());
        assertEquals(StateUsuario.ACTIVO, usuario.getValue().getState());

        ArgumentCaptor<UserRegistrationAttempt> intento = ArgumentCaptor.forClass(UserRegistrationAttempt.class);
        verify(attemptRepository).save(intento.capture());
        assertEquals(RegistrationStepType.AZURE_SYNC, intento.getValue().getStepType());
        assertTrue(intento.getValue().isSuccess());

        ArgumentCaptor<RegistrationOutboxEvent> outbox = ArgumentCaptor.forClass(RegistrationOutboxEvent.class);
        verify(outboxRepository).save(outbox.capture());
        assertEquals(RegistrationMessagingConstants.RK_REGISTER_ESTUDIANTE, outbox.getValue().getRoutingKey());
        assertEquals(OutboxEventState.PENDIENTE, outbox.getValue().getState());
        assertTrue(outbox.getValue().getPayload().contains(EMAIL));

        ArgumentCaptor<ProcessedRegistrationEvent> dedupe =
                ArgumentCaptor.forClass(ProcessedRegistrationEvent.class);
        verify(processedEventRepository).save(dedupe.capture());
        assertEquals("AZURE:" + EVENT_ID, dedupe.getValue().getEventId());
    }

    @Test
    void completarAzureEncolaAvisoDeCredencialesSiEstaHabilitado() {
        properties.setNotifyCredentialsEnabled(true);
        UserRegistrationProcess process = proceso();
        stubCompletarAzure(process);

        service.completarAzure(PROCESS_ID, EVENT_ID, OID, "Temp-123");

        ArgumentCaptor<RegistrationOutboxEvent> outbox = ArgumentCaptor.forClass(RegistrationOutboxEvent.class);
        verify(outboxRepository, times(2)).save(outbox.capture());
        List<RegistrationOutboxEvent> eventos = outbox.getAllValues();
        assertTrue(eventos.stream().anyMatch(e ->
                RegistrationMessagingConstants.RK_CREDENTIALS_NOTIFY.equals(e.getRoutingKey())));
        assertTrue(eventos.stream().anyMatch(e ->
                RegistrationMessagingConstants.RK_REGISTER_ESTUDIANTE.equals(e.getRoutingKey())));
        assertTrue(eventos.stream().noneMatch(e -> e.getPayload().contains("Temp-123")));
    }

    @Test
    void completarAzureReutilizaUsuarioExistenteYVinculaInvitacion() {
        UserRegistrationProcess process = proceso();
        process.setFullName(" ");
        Usuario existente = Usuario.builder().id(OID).email("viejo@test.com")
                .fullName("Nombre previo").rol(Rol.DOCENTE).state(StateUsuario.INACTIVO).build();
        InvitacionUsuario invitacion = InvitacionUsuario.builder()
                .email(EMAIL).rol(Rol.ESTUDIANTE).state(StateInvitacion.INVITADO).build();
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(existente));
        when(invitacionRepository.findByEmail(EMAIL)).thenReturn(Optional.of(invitacion));
        when(credentialCipher.cifrar("Temp-123")).thenReturn(new CredentialCipher.Cifrado("cifrado", "iv"));

        service.completarAzure(PROCESS_ID, EVENT_ID, OID, "Temp-123");

        assertEquals(EMAIL, existente.getEmail());
        assertEquals("Nombre previo", existente.getFullName());
        assertEquals(Rol.ESTUDIANTE, existente.getRol());
        assertEquals(StateUsuario.ACTIVO, existente.getState());
        assertEquals(StateInvitacion.VINCULADA, invitacion.getState());
        assertNotNull(invitacion.getBoundAt());
    }

    @Test
    void completarAzureNoHaceNadaSiLaEtapaYaTermino() {
        UserRegistrationProcess process = proceso();
        process.setAzureState(RegistrationStepState.COMPLETADO);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        service.completarAzure(PROCESS_ID, EVENT_ID, OID, "Temp-123");

        verify(usuarioRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
        verify(credentialCipher, never()).cifrar(anyString());
    }

    @Test
    void completarAzureLanzaErrorSiElRolAdminNoUsaRegistro() {
        UserRegistrationProcess process = proceso();
        process.setRequestedRole(Rol.ADMIN);
        stubCompletarAzure(process);

        assertThrows(IllegalStateException.class,
                () -> service.completarAzure(PROCESS_ID, EVENT_ID, OID, "Temp-123"));
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void completarAzureLanzaErrorSiRoleDataNoEsJsonValido() {
        UserRegistrationProcess process = proceso();
        process.setRoleData("no-es-json");
        stubCompletarAzure(process);

        assertThrows(IllegalStateException.class,
                () -> service.completarAzure(PROCESS_ID, EVENT_ID, OID, "Temp-123"));
    }

    @Test
    void completarAzureLanzaNotFoundSiElProcesoNoExiste() {
        when(processRepository.findById("desconocido")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.completarAzure("desconocido", EVENT_ID, OID, "Temp-123"));
    }

    @Test
    void fallarAzurePersisteElFalloYElDedupe() {
        UserRegistrationProcess process = proceso();
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        service.fallarAzure(PROCESS_ID, EVENT_ID, "Graph no disponible", true);

        assertEquals(RegistrationStepState.FALLIDO, process.getAzureState());
        assertEquals(RegistrationProcessState.FALLIDO, process.getState());
        assertEquals("Graph no disponible", process.getErrorMessage());
        verify(processedEventRepository).save(any());
    }

    @Test
    void fallarAzureSinMensajeUsaDetallePorDefectoYNoMarcaDedupe() {
        UserRegistrationProcess process = proceso();
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        service.fallarAzure(PROCESS_ID, EVENT_ID, " ", false);

        assertEquals("Sin detalle.", process.getErrorMessage());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void fallarAzureRecortaMensajesLargos() {
        UserRegistrationProcess process = proceso();
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        service.fallarAzure(PROCESS_ID, EVENT_ID, "x".repeat(1500), false);

        assertEquals(1000, process.getErrorMessage().length());
    }

    @Test
    void fallarAzureNoHaceNadaSiLaEtapaYaTermino() {
        UserRegistrationProcess process = proceso();
        process.setAzureState(RegistrationStepState.COMPLETADO);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        service.fallarAzure(PROCESS_ID, EVENT_ID, "error", true);

        verify(attemptRepository, never()).save(any());
        verify(processRepository, never()).save(any());
    }

    @Test
    void registrarIntentoTransitorioGuardaElIntento() {
        service.registrarIntentoTransitorio(PROCESS_ID, "reintentando");

        ArgumentCaptor<UserRegistrationAttempt> intento = ArgumentCaptor.forClass(UserRegistrationAttempt.class);
        verify(attemptRepository).save(intento.capture());
        assertEquals(PROCESS_ID, intento.getValue().getProcessId());
        assertEquals(RegistrationStepType.AZURE_SYNC, intento.getValue().getStepType());
        assertEquals("reintentando", intento.getValue().getMessage());
        assertTrue(!intento.getValue().isSuccess());
    }

    @Test
    void procesarResultadoDominioIgnoraEventosDuplicados() {
        when(processedEventRepository.existsById("DOMAIN:" + EVENT_ID)).thenReturn(true);

        service.procesarResultadoDominio(evento(true, "ok", null));

        verify(processRepository, never()).findById(any());
    }

    @Test
    void procesarResultadoDominioExitosoCompletaLaEtapa() {
        UserRegistrationProcess process = proceso();
        process.setAzureState(RegistrationStepState.COMPLETADO);
        when(processedEventRepository.existsById("DOMAIN:" + EVENT_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        service.procesarResultadoDominio(evento(true, "perfil creado", RegistrationStepType.AZURE_SYNC));

        assertEquals(RegistrationStepState.COMPLETADO, process.getDomainState());
        assertEquals(RegistrationProcessState.COMPLETADO, process.getState());
        assertNull(process.getErrorMessage());
        verify(processedEventRepository).save(any());
    }

    @Test
    void procesarResultadoDominioFallidoMarcaErrorYUsaTipoPorDefecto() {
        UserRegistrationProcess process = proceso();
        when(processedEventRepository.existsById("DOMAIN:" + EVENT_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        service.procesarResultadoDominio(evento(false, "no se pudo crear", null));

        assertEquals(RegistrationStepState.FALLIDO, process.getDomainState());
        assertEquals(RegistrationProcessState.FALLIDO, process.getState());
        assertEquals("no se pudo crear", process.getErrorMessage());

        ArgumentCaptor<UserRegistrationAttempt> intento = ArgumentCaptor.forClass(UserRegistrationAttempt.class);
        verify(attemptRepository).save(intento.capture());
        assertEquals(RegistrationStepType.DOMAIN_SYNC, intento.getValue().getStepType());
    }

    @Test
    void procesarResultadoDominioLanzaNotFoundSiElProcesoNoExiste() {
        when(processedEventRepository.existsById("DOMAIN:" + EVENT_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.procesarResultadoDominio(evento(true, "ok", null)));
    }

    @Test
    void fallarDominioMarcaLaEtapaYElDedupe() {
        UserRegistrationProcess process = proceso();
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        service.fallarDominio(PROCESS_ID, EVENT_ID, "cola agotada");

        assertEquals(RegistrationStepState.FALLIDO, process.getDomainState());
        assertEquals(RegistrationProcessState.FALLIDO, process.getState());
        assertEquals("cola agotada", process.getErrorMessage());
        verify(processedEventRepository).save(any());
    }

    @Test
    void fallarDominioNoHaceNadaSiYaSeCompleto() {
        UserRegistrationProcess process = proceso();
        process.setDomainState(RegistrationStepState.COMPLETADO);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        service.fallarDominio(PROCESS_ID, EVENT_ID, "error");

        verify(attemptRepository, never()).save(any());
        verify(processRepository, never()).save(any());
    }

    @Test
    void marcarPublicacionFallidaSinProcesoNoHaceNada() {
        service.marcarPublicacionFallida(null, "rk", "error");

        verify(processRepository, never()).findById(any());
    }

    @Test
    void marcarPublicacionFallidaIgnoraProcesosCompletadosOUNKNOWN() {
        UserRegistrationProcess completado = proceso();
        completado.setState(RegistrationProcessState.COMPLETADO);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(completado));
        service.marcarPublicacionFallida(PROCESS_ID, "rk", "error");
        verify(processRepository, never()).save(any());

        when(processRepository.findById("otro")).thenReturn(Optional.empty());
        service.marcarPublicacionFallida("otro", "rk", "error");
    }

    @Test
    void marcarPublicacionFallidaCierraElProceso() {
        UserRegistrationProcess process = proceso();
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        service.marcarPublicacionFallida(PROCESS_ID, "rk", "broker caido");

        assertEquals(RegistrationProcessState.FALLIDO, process.getState());
        assertTrue(process.getErrorMessage().contains("rk"));
        assertTrue(process.getErrorMessage().contains("broker caido"));
        verify(processRepository).save(process);
    }

    private UserRegistrationStepResultEventDTO evento(boolean success, String message, RegistrationStepType type) {
        return new UserRegistrationStepResultEventDTO(
                PROCESS_ID, EVENT_ID, type, success, message, OffsetDateTime.now());
    }
}
