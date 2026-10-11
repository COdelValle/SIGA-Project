package cl.siga.msusuariosauth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.enums.Parentesco;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioCompuestoRequestDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStatusResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.RegistrationProcessState;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroDocenteDTO;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroEstudianteDTO;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroApoderadoDTO;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroRolDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ConflictException;
import cl.siga.coreshare.exception.ServiceUnavailableException;
import cl.siga.coreshare.messaging.RegistrationMessagingConstants;
import cl.siga.msusuariosauth.config.RegistroAsyncProperties;
import cl.siga.msusuariosauth.integration.estudiantes.EstudianteDirectory;
import cl.siga.msusuariosauth.integration.graph.GraphUserDirectory;
import cl.siga.msusuariosauth.model.entity.RegistrationOutboxEvent;
import cl.siga.msusuariosauth.model.entity.UserRegistrationProcess;
import cl.siga.msusuariosauth.model.entity.Usuario;
import cl.siga.msusuariosauth.repository.ProcessedRegistrationEventRepository;
import cl.siga.msusuariosauth.repository.RegistrationOutboxEventRepository;
import cl.siga.msusuariosauth.repository.UserRegistrationAttemptRepository;
import cl.siga.msusuariosauth.repository.UserRegistrationProcessRepository;
import cl.siga.msusuariosauth.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AsyncUserRegistrationServiceTest {

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
    private GraphUserDirectory graphDirectory;
    @Mock
    private EstudianteDirectory estudianteDirectory;
    @Mock
    private RegistrationStateService stateService;
    @Mock
    private CredentialCipher credentialCipher;

    private final RegistroAsyncProperties properties = new RegistroAsyncProperties();
    private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    private AsyncUserRegistrationService service;

    @BeforeEach
    void setUp() {
        properties.setEnabled(true);
        properties.setCredentialKey("irrelevante-para-este-test");
        service = new AsyncUserRegistrationService(
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
                objectMapper,
                new EmailInstitucionalGenerator(properties));
    }

    private RegistrarUsuarioCompuestoRequestDTO requestEstudiante() {
        return new RegistrarUsuarioCompuestoRequestDTO(
                EMAIL,
                "Estudiante Nuevo",
                Rol.ESTUDIANTE,
                null,
                null,
                new DatosRegistroRolDTO(
                        new DatosRegistroEstudianteDTO(
                                "Ana", null, "Pérez", null, "12345678-9",
                                LocalDate.of(2012, 5, 1), null, null),
                        null,
                        null));
    }

    private RegistrarUsuarioCompuestoRequestDTO requestApoderado() {
        return new RegistrarUsuarioCompuestoRequestDTO(
                EMAIL,
                "Apoderado Nuevo",
                Rol.APODERADO,
                null,
                "contacto@test.com",
                new DatosRegistroRolDTO(
                        null,
                        null,
                        new DatosRegistroApoderadoDTO(
                                "Luis", null, "Soto", null, "11111111-1",
                                List.of("+56912345678"),
                                List.of(new ParentescoEstudianteDTO(42L, Parentesco.MADRE_PADRE)))));
    }

    @Test
    void iniciaRegistroYEncolaEventoAzure() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(processRepository.existsByEmailAndStateIn(anyString(), any())).thenReturn(false);
        when(processRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserRegistrationStatusResponseDTO estado = service.startRegistration(requestEstudiante(), "corr-1");

        assertEquals(RegistrationProcessState.EN_PROCESO, estado.state());
        assertEquals(Rol.ESTUDIANTE, estado.requestedRole());

        ArgumentCaptor<UserRegistrationProcess> proceso = ArgumentCaptor.forClass(UserRegistrationProcess.class);
        verify(processRepository).save(proceso.capture());
        assertEquals("corr-1", proceso.getValue().getCorrelationId());
        assertEquals(EMAIL, proceso.getValue().getEmail());
        assertNotNull(proceso.getValue().getRoleData());

        ArgumentCaptor<RegistrationOutboxEvent> outbox = ArgumentCaptor.forClass(RegistrationOutboxEvent.class);
        verify(outboxRepository).save(outbox.capture());
        assertEquals(RegistrationMessagingConstants.RK_AZURE_SYNC, outbox.getValue().getRoutingKey());
        assertTrue(outbox.getValue().getPayload().contains(EMAIL));
        verify(estudianteDirectory, never()).validarVinculos(any());
    }

    @Test
    void rechazaCuandoElFlujoEstaDeshabilitado() {
        properties.setEnabled(false);

        assertThrows(ServiceUnavailableException.class,
                () -> service.startRegistration(requestEstudiante(), null));
    }

    @Test
    void rechazaRolAdmin() {
        RegistrarUsuarioCompuestoRequestDTO request = new RegistrarUsuarioCompuestoRequestDTO(
                EMAIL, "Admin", Rol.ADMIN, null, null,
                new DatosRegistroRolDTO(null, null, null));

        assertThrows(BusinessException.class, () -> service.startRegistration(request, null));
    }

    @Test
    void rechazaPayloadQueNoCorrespondeAlRol() {
        RegistrarUsuarioCompuestoRequestDTO request = new RegistrarUsuarioCompuestoRequestDTO(
                EMAIL, "Docente", Rol.DOCENTE, null, null,
                new DatosRegistroRolDTO(
                        new DatosRegistroEstudianteDTO(
                                "Ana", null, "Pérez", null, "12345678-9",
                                LocalDate.of(2012, 5, 1), null, null),
                        null,
                        null));

        assertThrows(BusinessException.class, () -> service.startRegistration(request, null));
    }

    @Test
    void rechazaCorreoYaRegistradoYActivo() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(
                Usuario.builder().id("oid-activo").email(EMAIL).rol(Rol.ESTUDIANTE)
                        .state(StateUsuario.ACTIVO).build()));

        assertThrows(ConflictException.class,
                () -> service.startRegistration(requestEstudiante(), null));
        verify(processRepository, never()).save(any());
    }

    @Test
    void derivaCorreoYNombreCuandoNoVienen() {
        RegistrarUsuarioCompuestoRequestDTO request = new RegistrarUsuarioCompuestoRequestDTO(
                null, null, Rol.ESTUDIANTE, null, null,
                new DatosRegistroRolDTO(
                        new DatosRegistroEstudianteDTO(
                                "Ana", null, "Pérez", null, "12345678-9",
                                LocalDate.of(2012, 5, 1), null, null),
                        null, null));
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(processRepository.existsByEmailAndStateIn(anyString(), any())).thenReturn(false);
        when(processRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserRegistrationStatusResponseDTO estado = service.startRegistration(request, null);

        assertEquals("ana.perez@platformsiga.onmicrosoft.com", estado.email());
        ArgumentCaptor<UserRegistrationProcess> proceso = ArgumentCaptor.forClass(UserRegistrationProcess.class);
        verify(processRepository).save(proceso.capture());
        assertEquals("Ana Pérez", proceso.getValue().getFullName());
    }

    @Test
    void colisionDeCorreoActivoUsaSufijoNumerico() {
        RegistrarUsuarioCompuestoRequestDTO request = new RegistrarUsuarioCompuestoRequestDTO(
                null, null, Rol.ESTUDIANTE, null, null,
                new DatosRegistroRolDTO(
                        new DatosRegistroEstudianteDTO(
                                "Ana", null, "Pérez", null, "12345678-9",
                                LocalDate.of(2012, 5, 1), null, null),
                        null, null));
        Usuario activo = Usuario.builder()
                .id("otro-oid").email("ana.perez@platformsiga.onmicrosoft.com")
                .rol(Rol.ESTUDIANTE).state(StateUsuario.ACTIVO).build();
        when(usuarioRepository.findByEmail("ana.perez@platformsiga.onmicrosoft.com"))
                .thenReturn(Optional.of(activo));
        when(usuarioRepository.findByEmail("ana.perez2@platformsiga.onmicrosoft.com"))
                .thenReturn(Optional.empty());
        when(processRepository.existsByEmailAndStateIn(anyString(), any())).thenReturn(false);
        when(processRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserRegistrationStatusResponseDTO estado = service.startRegistration(request, null);

        assertEquals("ana.perez2@platformsiga.onmicrosoft.com", estado.email());
    }

    @Test
    void permiteReRegistroDeUsuarioInactivo() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(
                Usuario.builder().id("oid-inactivo").email(EMAIL).rol(Rol.ESTUDIANTE)
                        .state(StateUsuario.INACTIVO).build()));
        when(processRepository.existsByEmailAndStateIn(anyString(), any())).thenReturn(false);
        when(processRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserRegistrationStatusResponseDTO estado = service.startRegistration(requestEstudiante(), null);

        assertEquals(RegistrationProcessState.EN_PROCESO, estado.state());
        verify(outboxRepository).save(any());
    }

    @Test
    void rechazaProcesoEnCursoParaElMismoCorreo() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(processRepository.existsByEmailAndStateIn(anyString(), any())).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.startRegistration(requestEstudiante(), null));
    }

    @Test
    void validaVinculosDeApoderadoAntesDeAceptar() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(processRepository.existsByEmailAndStateIn(anyString(), any())).thenReturn(false);
        when(processRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.startRegistration(requestApoderado(), null);

        verify(estudianteDirectory).validarVinculos(any());
    }

    @Test
    void elEventoInicialNoLlevaCredencialesNiContacto() throws Exception {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(processRepository.existsByEmailAndStateIn(anyString(), any())).thenReturn(false);
        when(processRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.startRegistration(requestApoderado(), null);

        ArgumentCaptor<RegistrationOutboxEvent> outbox = ArgumentCaptor.forClass(RegistrationOutboxEvent.class);
        verify(outboxRepository).save(outbox.capture());
        String payload = outbox.getValue().getPayload();
        assertTrue(payload.contains("\"userId\":null"));
        assertFalse(payload.contains("contacto@test.com"));
        assertFalse(payload.contains("temporaryPassword"));
    }
}
