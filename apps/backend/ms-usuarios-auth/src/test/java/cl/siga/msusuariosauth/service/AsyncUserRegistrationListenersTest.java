package cl.siga.msusuariosauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

import com.rabbitmq.client.Channel;

import cl.siga.coreshare.config.CorrelationIdConfig;
import cl.siga.coreshare.dto.usuario.UserRegistrationEventDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStepResultEventDTO;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepType;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.mensajeria.ConfirmadorMensajes;
import cl.siga.coreshare.messaging.RegistrationMessagingConstants;

class AsyncUserRegistrationListenersTest {

    private AsyncUserRegistrationService service;
    private ConfirmadorMensajes confirmador;
    private AsyncUserRegistrationListeners listeners;
    private Channel canal;

    @BeforeEach
    void setUp() {
        service = mock(AsyncUserRegistrationService.class);
        confirmador = mock(ConfirmadorMensajes.class);
        listeners = new AsyncUserRegistrationListeners(service, confirmador);
        canal = mock(Channel.class);
    }

    private UserRegistrationEventDTO evento(String correlationId) {
        return new UserRegistrationEventDTO(
                UserRegistrationEventDTO.CURRENT_SCHEMA_VERSION, "evt-1", "proc-1",
                correlationId, "ana@test.com", "Ana", Rol.ESTUDIANTE, null, null,
                OffsetDateTime.now());
    }

    private UserRegistrationStepResultEventDTO resultado() {
        return new UserRegistrationStepResultEventDTO(
                "proc-1", "evt-2", RegistrationStepType.DOMAIN_SYNC, true, "ok",
                OffsetDateTime.now());
    }

    private Runnable capturarOperacion(String cola, long etiqueta) {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(confirmador).procesar(eq(canal), eq(etiqueta), eq(cola), captor.capture());
        return captor.getValue();
    }

    @Test
    void azureSyncUsaElHeaderDeCorrelacionYProcesaElEvento() {
        UserRegistrationEventDTO event = evento("fallback");
        AtomicReference<String> correlacion = new AtomicReference<>();
        doAnswer(inv -> {
            correlacion.set(MDC.get(CorrelationIdConfig.MDC_KEY));
            return null;
        }).when(service).processAzureSync(event);

        listeners.handleAzureSync(event, "header-corr", canal, 11L);
        capturarOperacion(RegistrationMessagingConstants.AZURE_QUEUE, 11L).run();

        assertThat(correlacion.get()).isEqualTo("header-corr");
        assertThat(MDC.get(CorrelationIdConfig.MDC_KEY)).isNull();
        verify(service).processAzureSync(event);
    }

    @Test
    void azureSyncUsaElFallbackDelEventoCuandoNoHayHeader() {
        UserRegistrationEventDTO event = evento("fallback");
        AtomicReference<String> correlacion = new AtomicReference<>();
        doAnswer(inv -> {
            correlacion.set(MDC.get(CorrelationIdConfig.MDC_KEY));
            return null;
        }).when(service).processAzureSync(event);

        listeners.handleAzureSync(event, " ", canal, 12L);
        capturarOperacion(RegistrationMessagingConstants.AZURE_QUEUE, 12L).run();

        assertThat(correlacion.get()).isEqualTo("fallback");
    }

    @Test
    void azureDlqProcesaYUsaLaCorrelacionDelEvento() {
        UserRegistrationEventDTO event = evento("corr-dlq");
        AtomicReference<String> correlacion = new AtomicReference<>();
        doAnswer(inv -> {
            correlacion.set(MDC.get(CorrelationIdConfig.MDC_KEY));
            return null;
        }).when(service).procesarAzureDlq(event);

        listeners.handleAzureDlq(event, canal, 13L);
        capturarOperacion(RegistrationMessagingConstants.AZURE_DLQ, 13L).run();

        assertThat(correlacion.get()).isEqualTo("corr-dlq");
        verify(service).procesarAzureDlq(event);
    }

    @Test
    void statusUsaElHeaderDeCorrelacionYProcesaElResultado() {
        UserRegistrationStepResultEventDTO event = resultado();
        AtomicReference<String> correlacion = new AtomicReference<>();
        doAnswer(inv -> {
            correlacion.set(MDC.get(CorrelationIdConfig.MDC_KEY));
            return null;
        }).when(service).processDomainStepStatus(event);

        listeners.handleDomainStatus(event, "corr-status", canal, 14L);
        capturarOperacion(RegistrationMessagingConstants.STATUS_QUEUE, 14L).run();

        assertThat(correlacion.get()).isEqualTo("corr-status");
        verify(service).processDomainStepStatus(event);
    }

    @Test
    void statusDlqProcesaSinCorrelacion() {
        UserRegistrationStepResultEventDTO event = resultado();
        AtomicReference<String> correlacion = new AtomicReference<>(null);
        doAnswer(inv -> {
            correlacion.set(MDC.get(CorrelationIdConfig.MDC_KEY));
            return null;
        }).when(service).procesarStatusDlq(event);

        listeners.handleStatusDlq(event, canal, 15L);
        capturarOperacion(RegistrationMessagingConstants.STATUS_DLQ, 15L).run();

        assertThat(correlacion.get()).isNull();
        verify(service).procesarStatusDlq(event);
    }
}
