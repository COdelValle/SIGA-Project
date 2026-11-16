package cl.siga.msusuariosauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cl.siga.coreshare.dto.usuario.enums.RegistrationProcessState;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepState;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.mensajeria.RegistroMensajeriaProperties;
import cl.siga.msusuariosauth.config.RegistroAsyncProperties;
import cl.siga.msusuariosauth.model.entity.RegistrationOutboxEvent;
import cl.siga.msusuariosauth.model.entity.UserRegistrationProcess;
import cl.siga.msusuariosauth.repository.InvitacionUsuarioRepository;
import cl.siga.msusuariosauth.repository.ProcessedRegistrationEventRepository;
import cl.siga.msusuariosauth.repository.RegistrationOutboxEventRepository;
import cl.siga.msusuariosauth.repository.UserRegistrationAttemptRepository;
import cl.siga.msusuariosauth.repository.UserRegistrationProcessRepository;
import cl.siga.msusuariosauth.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationStateServiceRolesTest {

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
    private final RegistroMensajeriaProperties messagingProperties = new RegistroMensajeriaProperties();
    private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    private RegistrationStateService service;

    @BeforeEach
    void setUp() {
        messagingProperties.setExchange("user.topic.exchange");
        messagingProperties.setRkRegisterEstudiante("user.register.estudiante");
        messagingProperties.setRkRegisterDocente("user.register.docente");
        messagingProperties.setRkRegisterApoderado("user.register.apoderado");
        messagingProperties.setRkCredentialsNotify("user.credentials.notify");
        service = new RegistrationStateService(processRepository, attemptRepository,
                processedEventRepository, outboxRepository, usuarioRepository,
                invitacionRepository, credentialCipher, properties, messagingProperties, objectMapper);
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(OID)).thenReturn(Optional.empty());
        when(invitacionRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(credentialCipher.cifrar(any())).thenReturn(new CredentialCipher.Cifrado("cifrado", "iv"));
    }

    private UserRegistrationProcess proceso(Rol rol) {
        return UserRegistrationProcess.builder()
                .processId(PROCESS_ID)
                .eventId(EVENT_ID)
                .correlationId("corr-1")
                .email(EMAIL)
                .fullName("Ana Pérez")
                .requestedRole(rol)
                .roleData("{}")
                .state(RegistrationProcessState.EN_PROCESO)
                .azureState(RegistrationStepState.PENDIENTE)
                .domainState(RegistrationStepState.PENDIENTE)
                .build();
    }

    private void completar(Rol rol) {
        UserRegistrationProcess process = proceso(rol);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        service.completarAzure(PROCESS_ID, EVENT_ID, OID, "Temp-123");
    }

    @Test
    void enrutaElEventoDeDominioDeDocenteYApoderado() {
        completar(Rol.DOCENTE);

        ArgumentCaptor<RegistrationOutboxEvent> outbox = ArgumentCaptor.forClass(RegistrationOutboxEvent.class);
        verify(outboxRepository).save(outbox.capture());
        assertThat(outbox.getValue().getRoutingKey())
                .isEqualTo(messagingProperties.getRkRegisterDocente());

        org.mockito.Mockito.clearInvocations(outboxRepository);
        completar(Rol.APODERADO);

        verify(outboxRepository).save(outbox.capture());
        assertThat(outbox.getValue().getRoutingKey())
                .isEqualTo(messagingProperties.getRkRegisterApoderado());
    }

    @Test
    void fallaSiNoPuedeSerializarElEventoDeDominio() throws Exception {
        ObjectMapper mapperRoto = mock(ObjectMapper.class);
        when(mapperRoto.readValue("{}", cl.siga.coreshare.dto.usuario.payload.DatosRegistroRolDTO.class))
                .thenReturn(null);
        when(mapperRoto.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("boom") { });
        RegistrationStateService servicioRoto = new RegistrationStateService(processRepository,
                attemptRepository, processedEventRepository, outboxRepository,
                usuarioRepository, invitacionRepository, credentialCipher, properties, messagingProperties, mapperRoto);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(proceso(Rol.ESTUDIANTE)));

        assertThatThrownBy(() -> servicioRoto.completarAzure(PROCESS_ID, EVENT_ID, OID, "Temp-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serializar");
    }
}
