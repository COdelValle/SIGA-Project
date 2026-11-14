package cl.siga.msusuariosauth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import cl.siga.msusuariosauth.config.RegistroAsyncProperties;
import cl.siga.msusuariosauth.model.entity.OutboxEventState;
import cl.siga.msusuariosauth.model.entity.RegistrationOutboxEvent;
import cl.siga.msusuariosauth.repository.RegistrationOutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationOutboxPublisherTest {

    @Mock
    private RegistrationOutboxEventRepository outboxRepository;
    @Mock
    private RegistrationStateService stateService;
    @Mock
    private RabbitTemplate rabbitTemplate;

    private final RegistroAsyncProperties properties = new RegistroAsyncProperties();

    private RegistrationOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        properties.setEnabled(true);
        properties.setOutboxConfirmTimeoutSeconds(1);
        publisher = new RegistrationOutboxPublisher(outboxRepository, stateService, rabbitTemplate, properties);
    }

    private RegistrationOutboxEvent evento(int attempts, String correlationId) {
        return RegistrationOutboxEvent.builder()
                .id(1L)
                .eventId("evt-1")
                .processId("proc-1")
                .exchange("siga.registro")
                .routingKey("user.registration.azure")
                .correlationId(correlationId)
                .payload("{\"hola\":\"mundo\"}")
                .state(OutboxEventState.PENDIENTE)
                .attempts(attempts)
                .build();
    }

    private void stubPendientes(RegistrationOutboxEvent evento) {
        when(outboxRepository.findTop20ByStateAndNextAttemptAtLessThanEqualOrderByIdAsc(
                eq(OutboxEventState.PENDIENTE), any(OffsetDateTime.class)))
                .thenReturn(List.of(evento));
    }

    private void confirmar(boolean ack, String reason) {
        doAnswer(inv -> {
            CorrelationData data = inv.getArgument(3);
            data.getFuture().complete(ack ? new CorrelationData.Confirm(true, null)
                    : new CorrelationData.Confirm(false, reason));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void noPublicaSiElFlujoEstaDeshabilitado() {
        properties.setEnabled(false);

        publisher.publicarPendientes();

        verifyNoInteractions(outboxRepository, rabbitTemplate, stateService);
    }

    @Test
    void noPublicaSiNoHayEventosPendientes() {
        when(outboxRepository.findTop20ByStateAndNextAttemptAtLessThanEqualOrderByIdAsc(
                eq(OutboxEventState.PENDIENTE), any(OffsetDateTime.class)))
                .thenReturn(List.of());

        publisher.publicarPendientes();

        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void publicaYMarcaElEventoComoEnviado() {
        RegistrationOutboxEvent evento = evento(0, "corr-1");
        stubPendientes(evento);
        confirmar(true, null);

        publisher.publicarPendientes();

        assertEquals(OutboxEventState.ENVIADO, evento.getState());
        assertNotNull(evento.getSentAt());
        assertNull(evento.getLastError());
        verify(outboxRepository).save(evento);

        ArgumentCaptor<Message> mensaje = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq("siga.registro"), eq("user.registration.azure"),
                mensaje.capture(), any(CorrelationData.class));
        assertEquals("evt-1", mensaje.getValue().getMessageProperties().getMessageId());
        assertEquals("corr-1", mensaje.getValue().getMessageProperties().getCorrelationId());
    }

    @Test
    void publicaSinCorrelationIdCuandoNoViene() {
        RegistrationOutboxEvent evento = evento(0, null);
        stubPendientes(evento);
        confirmar(true, null);

        publisher.publicarPendientes();

        assertEquals(OutboxEventState.ENVIADO, evento.getState());
    }

    @Test
    void rechazoDelBrokerIncrementaIntentosYProgramaReintento() {
        RegistrationOutboxEvent evento = evento(0, "corr-1");
        stubPendientes(evento);
        confirmar(false, "cola llena");

        publisher.publicarPendientes();

        assertEquals(1, evento.getAttempts());
        assertEquals(OutboxEventState.PENDIENTE, evento.getState());
        assertTrue(evento.getLastError().contains("cola llena"));
        assertNotNull(evento.getNextAttemptAt());
        verify(stateService, never()).marcarPublicacionFallida(any(), any(), any());
        verify(outboxRepository).save(evento);
    }

    @Test
    void confirmacionNulaSeConsideraFallo() {
        RegistrationOutboxEvent evento = evento(0, "corr-1");
        stubPendientes(evento);
        doAnswer(inv -> {
            CorrelationData data = inv.getArgument(3);
            data.getFuture().complete(null);
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        publisher.publicarPendientes();

        assertEquals(1, evento.getAttempts());
        assertTrue(evento.getLastError().contains("no confirmó"));
    }

    @Test
    void excepcionAlEnviarSeRegistraYProgramaReintento() {
        RegistrationOutboxEvent evento = evento(0, "corr-1");
        stubPendientes(evento);
        doThrow(new IllegalStateException("broker caido"))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        publisher.publicarPendientes();

        assertEquals(1, evento.getAttempts());
        assertEquals("broker caido", evento.getLastError());
        assertEquals(OutboxEventState.PENDIENTE, evento.getState());
        assertNotNull(evento.getNextAttemptAt());
    }

    @Test
    void excepcionSinMensajeUsaElNombreDeLaClase() {
        RegistrationOutboxEvent evento = evento(0, "corr-1");
        stubPendientes(evento);
        doThrow(new IllegalStateException())
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        publisher.publicarPendientes();

        assertEquals("IllegalStateException", evento.getLastError());
    }

    @Test
    void recortaElErrorA1000Caracteres() {
        RegistrationOutboxEvent evento = evento(0, "corr-1");
        stubPendientes(evento);
        doThrow(new IllegalStateException("x".repeat(1500)))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        publisher.publicarPendientes();

        assertEquals(1000, evento.getLastError().length());
    }

    @Test
    void alAgotarIntentosMarcaFallidoYCierraElProceso() {
        properties.setOutboxMaxAttempts(3);
        RegistrationOutboxEvent evento = evento(2, "corr-1");
        stubPendientes(evento);
        doThrow(new IllegalStateException("sin broker"))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        publisher.publicarPendientes();

        assertEquals(3, evento.getAttempts());
        assertEquals(OutboxEventState.FALLIDO, evento.getState());
        verify(stateService).marcarPublicacionFallida("proc-1", "user.registration.azure", "sin broker");
        verify(outboxRepository).save(evento);
    }
}
