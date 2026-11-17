package cl.siga.msusuariosauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroDocenteDTO;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroEstudianteDTO;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroRolDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ConflictException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.exception.ServiceUnavailableException;
import cl.siga.coreshare.mensajeria.RegistroMensajeriaProperties;
import cl.siga.msusuariosauth.config.RegistroAsyncProperties;
import cl.siga.msusuariosauth.integration.estudiantes.EstudianteDirectory;
import cl.siga.msusuariosauth.integration.graph.GraphTransientException;
import cl.siga.msusuariosauth.integration.graph.GraphUserDirectory;
import cl.siga.msusuariosauth.integration.graph.ProvisionedUser;
import cl.siga.msusuariosauth.model.entity.UserRegistrationProcess;
import cl.siga.msusuariosauth.model.entity.Usuario;
import cl.siga.msusuariosauth.repository.ProcessedRegistrationEventRepository;
import cl.siga.msusuariosauth.repository.RegistrationOutboxEventRepository;
import cl.siga.msusuariosauth.repository.UserRegistrationAttemptRepository;
import cl.siga.msusuariosauth.repository.UserRegistrationProcessRepository;
import cl.siga.msusuariosauth.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsyncUserRegistrationServiceExtraTest {

    private static final String EMAIL = "nuevo@test.com";
    private static final String PROCESS_ID = "process-1";
    private static final String EVENT_ID = "event-1";

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
    private GraphUserDirectory graphDirectory;
    @Mock
    private EstudianteDirectory estudianteDirectory;
    @Mock
    private RegistrationStateService stateService;
    @Mock
    private CredentialCipher credentialCipher;

    private final RegistroAsyncProperties properties = new RegistroAsyncProperties();
    private final RegistroMensajeriaProperties messagingProperties = new RegistroMensajeriaProperties();
    private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    private AsyncUserRegistrationService service;

    @BeforeEach
    void setUp() {
        properties.setEnabled(true);
        properties.setCredentialKey("irrelevante-para-este-test");
        properties.setEmailDomain("platformsiga.onmicrosoft.com");
        messagingProperties.setExchange("user.topic.exchange");
        messagingProperties.setRkAzureSync("user.registration.azure");
        service = crearService(objectMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private AsyncUserRegistrationService crearService(ObjectMapper mapper) {
        return new AsyncUserRegistrationService(
                processRepository,
                attemptRepository,
                processedEventRepository,
                outboxRepository,
                usuarioRepository,
                graphDirectory,
                estudianteDirectory,
                stateService,
                credentialCipher,
                properties,
                messagingProperties,
                mapper,
                new EmailInstitucionalGenerator(properties));
    }

    private void autenticar(String oid) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("oid", oid).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private DatosRegistroRolDTO datosEstudiante(String segundoApellido) {
        return new DatosRegistroRolDTO(
                new DatosRegistroEstudianteDTO(
                        "Ana", null, "Pérez", segundoApellido, "12345678-9",
                        LocalDate.of(2012, 5, 1), null, null),
                null, null);
    }

    private RegistrarUsuarioCompuestoRequestDTO requestEstudiante(String email, String fullName) {
        return new RegistrarUsuarioCompuestoRequestDTO(
                email, fullName, Rol.ESTUDIANTE, null, null, datosEstudiante(null));
    }

    private UserRegistrationProcess proceso() {
        return UserRegistrationProcess.builder()
                .processId(PROCESS_ID)
                .eventId(EVENT_ID)
                .correlationId("corr-1")
                .email(EMAIL)
                .fullName("Ana Pérez")
                .requestedRole(Rol.ESTUDIANTE)
                .state(RegistrationProcessState.EN_PROCESO)
                .azureState(RegistrationStepState.COMPLETADO)
                .domainState(RegistrationStepState.PENDIENTE)
                .build();
    }

    private UserRegistrationEventDTO evento(String schemaVersion, String correlationId) {
        return new UserRegistrationEventDTO(
                schemaVersion, EVENT_ID, PROCESS_ID, correlationId, EMAIL, "Ana Pérez",
                Rol.ESTUDIANTE, "oid-1", datosEstudiante(null), OffsetDateTime.now());
    }

    @Test
    void getStatusDevuelveElProcesoYFallaSiNoExiste() {
        UserRegistrationProcess process = proceso();
        process.setAzureState(RegistrationStepState.PENDIENTE);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        UserRegistrationStatusResponseDTO estado = service.getStatus(PROCESS_ID);

        assertThat(estado.processId()).isEqualTo(PROCESS_ID);
        assertThat(estado.azureState()).isEqualTo(RegistrationStepState.PENDIENTE);

        when(processRepository.findById("otro")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getStatus("otro"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerCredencialLaEntregaUnaSolaVezAlDueno() {
        autenticar("admin-1");
        UserRegistrationProcess process = proceso();
        process.setCreatedBy("admin-1");
        process.setCredentialCiphertext("cifrado");
        process.setCredentialIv("iv");
        process.setCredentialExpiresAt(OffsetDateTime.now().plusHours(1));
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(credentialCipher.descifrar("cifrado", "iv")).thenReturn("Temp-123");

        UserRegistrationCredentialResponseDTO credencial = service.obtenerCredencial(PROCESS_ID);

        assertThat(credencial.temporaryPassword()).isEqualTo("Temp-123");
        assertThat(credencial.email()).isEqualTo(EMAIL);
        assertThat(process.getCredentialRetrievedAt()).isNotNull();
        assertThat(process.getCredentialCiphertext()).isNull();
        assertThat(process.getCredentialIv()).isNull();
        assertThat(process.getCredentialExpiresAt()).isNull();
        verify(processRepository).save(process);
    }

    @Test
    void obtenerCredencialRechazaAQuienNoInicioElProceso() {
        autenticar("admin-2");
        UserRegistrationProcess process = proceso();
        process.setCreatedBy("admin-1");
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> service.obtenerCredencial(PROCESS_ID))
                .isInstanceOf(AccessDeniedException.class);
        verify(credentialCipher, never()).descifrar(anyString(), anyString());
    }

    @Test
    void obtenerCredencialRechazaCuentaNoAprovisionada() {
        UserRegistrationProcess process = proceso();
        process.setAzureState(RegistrationStepState.PENDIENTE);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> service.obtenerCredencial(PROCESS_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("todavía no está aprovisionada");
    }

    @Test
    void obtenerCredencialRechazaEntregaRepetidaOCredencialVacia() {
        UserRegistrationProcess yaEntregada = proceso();
        yaEntregada.setCredentialRetrievedAt(OffsetDateTime.now());
        yaEntregada.setCredentialCiphertext("cifrado");
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(yaEntregada));
        assertThatThrownBy(() -> service.obtenerCredencial(PROCESS_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya fue entregada");

        UserRegistrationProcess sinCredencial = proceso();
        sinCredencial.setCredentialCiphertext(null);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(sinCredencial));
        assertThatThrownBy(() -> service.obtenerCredencial(PROCESS_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya fue entregada");
    }

    @Test
    void obtenerCredencialExpiradaLaLimpiaYRechaza() {
        UserRegistrationProcess process = proceso();
        process.setCredentialCiphertext("cifrado");
        process.setCredentialIv("iv");
        process.setCredentialExpiresAt(OffsetDateTime.now().minusHours(1));
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> service.obtenerCredencial(PROCESS_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("expiró");
        assertThat(process.getCredentialCiphertext()).isNull();
        verify(processRepository).save(process);
        verify(credentialCipher, never()).descifrar(anyString(), anyString());
    }

    @Test
    void obtenerCredencialFallaSiElProcesoNoExiste() {
        when(processRepository.findById("otro")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerCredencial("otro"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resetPasswordFallaSiElUsuarioNoExisteOEstaInactivo() {
        when(usuarioRepository.findById("sin")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resetPassword("sin"))
                .isInstanceOf(ResourceNotFoundException.class);

        Usuario inactivo = Usuario.builder().id("inactivo").email(EMAIL)
                .rol(Rol.DOCENTE).state(StateUsuario.INACTIVO).build();
        when(usuarioRepository.findById("inactivo")).thenReturn(Optional.of(inactivo));
        assertThatThrownBy(() -> service.resetPassword("inactivo"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(graphDirectory, never()).resetPassword(anyString());
    }

    @Test
    void resetPasswordFallaSiGraphNoEstaConfigurado() {
        Usuario usuario = Usuario.builder().id("oid-1").email(EMAIL)
                .rol(Rol.DOCENTE).state(StateUsuario.ACTIVO).build();
        when(usuarioRepository.findById("oid-1")).thenReturn(Optional.of(usuario));
        when(graphDirectory.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> service.resetPassword("oid-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Microsoft Graph");
    }

    @Test
    void resetPasswordRegeneraLaClaveTemporal() {
        Usuario usuario = Usuario.builder().id("oid-1").email(EMAIL)
                .rol(Rol.DOCENTE).state(StateUsuario.ACTIVO).build();
        when(usuarioRepository.findById("oid-1")).thenReturn(Optional.of(usuario));
        when(graphDirectory.isEnabled()).thenReturn(true);
        when(graphDirectory.resetPassword("oid-1")).thenReturn("Temp-456");

        UserRegistrationCredentialResponseDTO credencial = service.resetPassword("oid-1");

        assertThat(credencial.processId()).isNull();
        assertThat(credencial.email()).isEqualTo(EMAIL);
        assertThat(credencial.userId()).isEqualTo("oid-1");
        assertThat(credencial.temporaryPassword()).isEqualTo("Temp-456");
    }

    @Test
    void processAzureSyncRechazaEsquemaNoSoportado() {
        service.processAzureSync(evento("v2", "corr-1"));

        verify(stateService).fallarAzure(PROCESS_ID, EVENT_ID,
                "Versión de evento no soportada: v2", true);
        verify(graphDirectory, never()).provisionUser(any(), any(), any(), any());
    }

    @Test
    void processAzureSyncIgnoraEventosDuplicados() {
        when(processedEventRepository.existsById("AZURE:" + EVENT_ID)).thenReturn(true);

        service.processAzureSync(evento(UserRegistrationEventDTO.CURRENT_SCHEMA_VERSION, "corr-1"));

        verify(processRepository, never()).findById(anyString());
        verify(graphDirectory, never()).provisionUser(any(), any(), any(), any());
    }

    @Test
    void processAzureSyncIgnoraProcesosYaCompletadosOFallidos() {
        UserRegistrationProcess completado = proceso();
        when(processedEventRepository.existsById("AZURE:" + EVENT_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(completado));

        service.processAzureSync(evento(UserRegistrationEventDTO.CURRENT_SCHEMA_VERSION, "corr-1"));
        verify(graphDirectory, never()).provisionUser(any(), any(), any(), any());

        UserRegistrationProcess fallido = proceso();
        fallido.setState(RegistrationProcessState.FALLIDO);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(fallido));

        service.processAzureSync(evento(UserRegistrationEventDTO.CURRENT_SCHEMA_VERSION, "corr-1"));
        verify(graphDirectory, never()).provisionUser(any(), any(), any(), any());
    }

    @Test
    void processAzureSyncCompletaLaCuentaAprovisionada() {
        UserRegistrationProcess process = proceso();
        process.setAzureState(RegistrationStepState.PENDIENTE);
        when(processedEventRepository.existsById("AZURE:" + EVENT_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(graphDirectory.provisionUser(EMAIL, "Ana Pérez", Rol.ESTUDIANTE, "oid-1"))
                .thenReturn(new ProvisionedUser("oid-1", "Temp-789"));

        service.processAzureSync(evento(UserRegistrationEventDTO.CURRENT_SCHEMA_VERSION, "corr-1"));

        verify(stateService).completarAzure(PROCESS_ID, EVENT_ID, "oid-1", "Temp-789");
    }

    @Test
    void processAzureSyncPropagaErrorTransitorioYLoRegistra() {
        UserRegistrationProcess process = proceso();
        process.setAzureState(RegistrationStepState.PENDIENTE);
        when(processedEventRepository.existsById("AZURE:" + EVENT_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        GraphTransientException error = new GraphTransientException("Graph caido");
        when(graphDirectory.provisionUser(any(), any(), any(), any())).thenThrow(error);

        assertThatThrownBy(() -> service.processAzureSync(
                evento(UserRegistrationEventDTO.CURRENT_SCHEMA_VERSION, "corr-1")))
                .isSameAs(error);
        verify(stateService).registrarIntentoTransitorio(PROCESS_ID, "Graph caido");
    }

    @Test
    void processAzureSyncUsaMensajeSeguroCuandoElErrorTransitorioNoTieneDetalle() {
        UserRegistrationProcess process = proceso();
        process.setAzureState(RegistrationStepState.PENDIENTE);
        when(processedEventRepository.existsById("AZURE:" + EVENT_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(graphDirectory.provisionUser(any(), any(), any(), any()))
                .thenThrow(new GraphTransientException(" "));

        assertThatThrownBy(() -> service.processAzureSync(
                evento(UserRegistrationEventDTO.CURRENT_SCHEMA_VERSION, "corr-1")))
                .isInstanceOf(GraphTransientException.class);
        verify(stateService).registrarIntentoTransitorio(PROCESS_ID,
                "Error de aprovisionamiento en Entra ID.");
    }

    @Test
    void processAzureSyncMarcaFalloPermanenteCuandoGraphRechazaLaCuenta() {
        UserRegistrationProcess process = proceso();
        process.setAzureState(RegistrationStepState.PENDIENTE);
        when(processedEventRepository.existsById("AZURE:" + EVENT_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(graphDirectory.provisionUser(any(), any(), any(), any()))
                .thenThrow(new BusinessException("cuenta duplicada"));

        service.processAzureSync(evento(UserRegistrationEventDTO.CURRENT_SCHEMA_VERSION, "corr-1"));

        verify(stateService).fallarAzure(PROCESS_ID, EVENT_ID, "cuenta duplicada", true);
    }

    @Test
    void processAzureSyncRecortaElDetalleDelErrorPermanente() {
        UserRegistrationProcess process = proceso();
        process.setAzureState(RegistrationStepState.PENDIENTE);
        when(processedEventRepository.existsById("AZURE:" + EVENT_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(graphDirectory.provisionUser(any(), any(), any(), any()))
                .thenThrow(new BusinessException("x".repeat(1500)));

        service.processAzureSync(evento(UserRegistrationEventDTO.CURRENT_SCHEMA_VERSION, "corr-1"));

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(stateService).fallarAzure(eq(PROCESS_ID), eq(EVENT_ID), mensaje.capture(), eq(true));
        assertThat(mensaje.getValue()).hasSize(1000);
    }

    @Test
    void processAzureSyncPropagaErrorInesperadoYRegistraElIntento() {
        UserRegistrationProcess process = proceso();
        process.setAzureState(RegistrationStepState.PENDIENTE);
        when(processedEventRepository.existsById("AZURE:" + EVENT_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(graphDirectory.provisionUser(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> service.processAzureSync(
                evento(UserRegistrationEventDTO.CURRENT_SCHEMA_VERSION, "corr-1")))
                .isInstanceOf(IllegalStateException.class);
        verify(stateService).registrarIntentoTransitorio(PROCESS_ID,
                "Error inesperado durante el aprovisionamiento.");
    }

    @Test
    void procesaDlqYResultadosDeDominioDelegandoEnElEstado() {
        service.procesarAzureDlq(evento(UserRegistrationEventDTO.CURRENT_SCHEMA_VERSION, "corr-1"));
        verify(stateService).fallarAzure(PROCESS_ID, EVENT_ID,
                "Se agotaron los reintentos de aprovisionamiento en Entra ID.", true);

        UserRegistrationStepResultEventDTO resultado = new UserRegistrationStepResultEventDTO(
                PROCESS_ID, EVENT_ID, RegistrationStepType.DOMAIN_SYNC, true, "ok", OffsetDateTime.now());
        service.processDomainStepStatus(resultado);
        verify(stateService).procesarResultadoDominio(resultado);

        service.procesarStatusDlq(resultado);
        verify(stateService).fallarDominio(PROCESS_ID, EVENT_ID,
                "Se agotaron los reintentos al procesar el resultado de la etapa de dominio.");
    }

    @Test
    void startRegistrationRechazaRolNuloPayloadNuloYMultiplesBloques() {
        RegistrarUsuarioCompuestoRequestDTO sinRol = new RegistrarUsuarioCompuestoRequestDTO(
                EMAIL, "Ana", null, null, null, datosEstudiante(null));
        assertThatThrownBy(() -> service.startRegistration(sinRol, null))
                .isInstanceOf(BusinessException.class);

        RegistrarUsuarioCompuestoRequestDTO sinPayload = new RegistrarUsuarioCompuestoRequestDTO(
                EMAIL, "Ana", Rol.ESTUDIANTE, null, null, null);
        assertThatThrownBy(() -> service.startRegistration(sinPayload, null))
                .isInstanceOf(BusinessException.class);

        DatosRegistroDocenteDTO docente = new DatosRegistroDocenteDTO(
                "Pedro", null, "Soto", null, "11111111-1", LocalDate.of(2020, 1, 1), null, null);
        RegistrarUsuarioCompuestoRequestDTO multiple = new RegistrarUsuarioCompuestoRequestDTO(
                EMAIL, "Ana", Rol.ESTUDIANTE, null, null,
                new DatosRegistroRolDTO(datosEstudiante(null).estudiante(), docente, null));
        assertThatThrownBy(() -> service.startRegistration(multiple, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exactamente un bloque");
    }

    @Test
    void startRegistrationRechazaCuandoElFlujoEstaDeshabilitado() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> service.startRegistration(requestEstudiante(EMAIL, "Ana"), null))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void startRegistrationPruebaSegundoApellidoCuandoElCorreoBaseEstaEnUnProceso() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(processRepository.existsByEmailAndStateIn(
                eq("ana.perez@platformsiga.onmicrosoft.com"), any())).thenReturn(true);
        when(processRepository.existsByEmailAndStateIn(
                eq("ana.perezsoto@platformsiga.onmicrosoft.com"), any())).thenReturn(false);
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        RegistrarUsuarioCompuestoRequestDTO request = new RegistrarUsuarioCompuestoRequestDTO(
                null, null, Rol.ESTUDIANTE, null, null, datosEstudiante("Soto"));

        UserRegistrationStatusResponseDTO estado = service.startRegistration(request, null);

        assertThat(estado.email()).isEqualTo("ana.perezsoto@platformsiga.onmicrosoft.com");
    }

    @Test
    void startRegistrationConsultaGraphParaResolverColisionesDeCorreo() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(processRepository.existsByEmailAndStateIn(anyString(), any())).thenReturn(false);
        when(graphDirectory.accountEnabledByEmail("ana.perez@platformsiga.onmicrosoft.com"))
                .thenReturn(Optional.of(true));
        when(graphDirectory.accountEnabledByEmail("ana.perez2@platformsiga.onmicrosoft.com"))
                .thenReturn(Optional.of(false));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        RegistrarUsuarioCompuestoRequestDTO request = new RegistrarUsuarioCompuestoRequestDTO(
                null, null, Rol.ESTUDIANTE, null, null, datosEstudiante(null));

        UserRegistrationStatusResponseDTO estado = service.startRegistration(request, null);

        assertThat(estado.email()).isEqualTo("ana.perez2@platformsiga.onmicrosoft.com");
    }

    @Test
    void startRegistrationNormalizaDatosOpcionalesYUsaElProcessIdComoCorrelacion() {
        autenticar("admin-oid");
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(processRepository.existsByEmailAndStateIn(anyString(), any())).thenReturn(false);
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        RegistrarUsuarioCompuestoRequestDTO request = new RegistrarUsuarioCompuestoRequestDTO(
                "  MiXeD@Test.COM ", "   ", Rol.ESTUDIANTE, "  ", "   ", datosEstudiante(null));

        UserRegistrationStatusResponseDTO estado = service.startRegistration(request, "   ");

        ArgumentCaptor<UserRegistrationProcess> captor =
                ArgumentCaptor.forClass(UserRegistrationProcess.class);
        verify(processRepository).save(captor.capture());
        UserRegistrationProcess process = captor.getValue();
        assertThat(estado.email()).isEqualTo("mixed@test.com");
        assertThat(process.getEmail()).isEqualTo("mixed@test.com");
        assertThat(process.getFullName()).isEqualTo("Ana Pérez");
        assertThat(process.getCorrelationId()).isEqualTo(process.getProcessId());
        assertThat(process.getCreatedBy()).isEqualTo("admin-oid");
        assertThat(process.getContactEmail()).isNull();
        assertThat(process.getUserId()).isNull();
    }

    @Test
    void startRegistrationFallaSiNoPuedeSerializarElPayload() throws Exception {
        ObjectMapper mapper = org.mockito.Mockito.mock(ObjectMapper.class);
        when(mapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") { });
        AsyncUserRegistrationService servicioRoto = crearService(mapper);
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(processRepository.existsByEmailAndStateIn(anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> servicioRoto.startRegistration(requestEstudiante(EMAIL, "Ana"), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serializar");
        verify(processRepository, never()).save(any());
    }

    @Test
    void startRegistrationFallaSiLaSerializacionDelEventoNoEsPosible() throws Exception {
        ObjectMapper mapper = org.mockito.Mockito.mock(ObjectMapper.class);
        when(mapper.writeValueAsString(any()))
                .thenReturn("{}")
                .thenThrow(new JsonProcessingException("boom") { });
        AsyncUserRegistrationService servicioRoto = crearService(mapper);
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(processRepository.existsByEmailAndStateIn(anyString(), any())).thenReturn(false);
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> servicioRoto.startRegistration(requestEstudiante(EMAIL, "Ana"), null))
                .isInstanceOf(IllegalStateException.class);
    }
}
