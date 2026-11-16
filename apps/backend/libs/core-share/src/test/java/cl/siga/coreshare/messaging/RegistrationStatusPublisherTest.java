package cl.siga.coreshare.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import cl.siga.coreshare.dto.usuario.UserRegistrationStepResultEventDTO;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepType;
import cl.siga.coreshare.mensajeria.RegistroMensajeriaProperties;

class RegistrationStatusPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private RegistrationStatusPublisher publisher;
    private RegistroMensajeriaProperties properties;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        properties = RegistrationMessagingConfigTest.registro();
        publisher = new RegistrationStatusPublisher(rabbitTemplate, properties);
    }

    private void responderCon(CorrelationData.Confirm confirmacion, boolean completar) {
        doAnswer(invocacion -> {
            CorrelationData correlationData = invocacion.getArgument(4);
            if (completar) {
                correlationData.getFuture().complete(confirmacion);
            }
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    private MessagePostProcessor capturarPostProcessor() {
        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class),
                captor.capture(), any(CorrelationData.class));
        return captor.getValue();
    }

    private UserRegistrationStepResultEventDTO capturarEvento() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), captor.capture(),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        return (UserRegistrationStepResultEventDTO) captor.getValue();
    }

    @Test
    void publicaResultadoConConfirmacionYCorrelationId() throws Exception {
        responderCon(new CorrelationData.Confirm(true, null), true);

        publisher.publicarResultado("proc-1", "corr-1", true, "perfil creado");

        UserRegistrationStepResultEventDTO evento = capturarEvento();
        assertThat(evento.processId()).isEqualTo("proc-1");
        assertThat(evento.success()).isTrue();
        assertThat(evento.stepType()).isEqualTo(RegistrationStepType.DOMAIN_SYNC);
        assertThat(evento.message()).isEqualTo("perfil creado");
        verify(rabbitTemplate).convertAndSend(eq(properties.getExchange()),
                eq(properties.getRkStatus()), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        Message mensaje = new Message(new byte[0]);
        capturarPostProcessor().postProcessMessage(mensaje);
        assertThat(mensaje.getMessageProperties().getMessageId()).isEqualTo(evento.eventId());
        assertThat(mensaje.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(mensaje.getMessageProperties().getCorrelationId()).isEqualTo("corr-1");
        Object correlationHeader = mensaje.getMessageProperties()
                .getHeader(properties.getHeaderCorrelationId());
        assertThat(correlationHeader).isEqualTo("corr-1");
    }

    @Test
    void sinCorrelationIdNoAgregaHeadersDeCorrelacion() throws Exception {
        responderCon(new CorrelationData.Confirm(true, null), true);

        publisher.publicarResultado("proc-1", "   ", true, null);

        Message mensaje = new Message(new byte[0]);
        capturarPostProcessor().postProcessMessage(mensaje);
        assertThat(mensaje.getMessageProperties().getCorrelationId()).isNull();
        Object correlationHeader = mensaje.getMessageProperties()
                .getHeader(properties.getHeaderCorrelationId());
        assertThat(correlationHeader).isNull();
        assertThat(capturarEvento().message()).isEqualTo("Sin detalle.");
    }

    @Test
    void recortaElMensajeA1000Caracteres() {
        responderCon(new CorrelationData.Confirm(true, null), true);

        publisher.publicarResultado("proc-1", null, false, "x".repeat(1500));

        assertThat(capturarEvento().message()).hasSize(1000);
    }

    @Test
    void brokerQueRechazaLanzaExcepcionTransitoria() {
        responderCon(new CorrelationData.Confirm(false, "cola llena"), true);

        assertThatThrownBy(() -> publisher.publicarResultado("proc-1", null, true, "ok"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rechazó")
                .hasMessageContaining("cola llena");
    }

    @Test
    void brokerQueNoConfirmaLanzaExcepcionTransitoria() {
        responderCon(null, true);

        assertThatThrownBy(() -> publisher.publicarResultado("proc-1", null, true, "ok"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no confirmó");
    }

    @Test
    void futureFallidoLanzaExcepcionTransitoria() {
        doAnswer(invocacion -> {
            CorrelationData correlationData = invocacion.getArgument(4);
            correlationData.getFuture().completeExceptionally(new IllegalStateException("timeout"));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThatThrownBy(() -> publisher.publicarResultado("proc-1", null, true, "ok"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Sin confirmación");
    }

    @Test
    void hiloInterrumpidoLanzaExcepcionYRestauraElFlag() {
        responderCon(null, false);
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> publisher.publicarResultado("proc-1", null, true, "ok"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Interrumpido");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
