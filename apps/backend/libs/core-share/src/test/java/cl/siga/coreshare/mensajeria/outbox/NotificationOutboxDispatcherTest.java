package cl.siga.coreshare.mensajeria.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cl.siga.coreshare.dto.notificaciones.EventoNota;

class NotificationOutboxDispatcherTest {

    private JdbcTemplate jdbcTemplate;
    private RabbitTemplate rabbitTemplate;
    private ObjectMapper objectMapper;
    private NotificationOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        dispatcher = new NotificationOutboxDispatcher(jdbcTemplate, objectMapper, rabbitTemplate);
    }

    private void prepararFilas(String[][] filas) {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenAnswer(invocacion -> {
            RowMapper<?> mapper = invocacion.getArgument(1);
            List<Object> resultado = new ArrayList<>();
            for (int i = 0; i < filas.length; i++) {
                resultado.add(mapper.mapRow(resultSet(filas[i]), i));
            }
            return resultado;
        });
    }

    private ResultSet resultSet(String[] fila) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("id_evento")).thenReturn(fila[0]);
        when(rs.getString("intercambio")).thenReturn(fila[1]);
        when(rs.getString("clave")).thenReturn(fila[2]);
        when(rs.getString("tipo_payload")).thenReturn(fila[3]);
        when(rs.getString("payload")).thenReturn(fila[4]);
        when(rs.getInt("intentos")).thenReturn(Integer.parseInt(fila[5]));
        return rs;
    }

    private String[] fila(String idEvento, String tipoPayload, String payload, int intentos) {
        return new String[] {idEvento, "exchange.notificaciones", "nota.creada", tipoPayload, payload,
                String.valueOf(intentos)};
    }

    private void responderPublicacion(CorrelationData.Confirm confirmacion, ReturnedMessage retornado) {
        doAnswer(invocacion -> {
            CorrelationData correlationData = invocacion.getArgument(3);
            if (retornado != null) {
                correlationData.setReturned(retornado);
            }
            correlationData.getFuture().complete(confirmacion);
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class),
                any(CorrelationData.class));
    }

    private void responderPublicacionExitosa() {
        responderPublicacion(new CorrelationData.Confirm(true, null), null);
    }

    @Test
    void sinFilasPendientesNoPublicaNiActualiza() {
        prepararFilas(new String[0][]);

        dispatcher.publicarPendientes();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class),
                any(CorrelationData.class));
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void publicaEventoNotaConfirmadoYMarcaEnviado() {
        prepararFilas(new String[][] {fila("evt-1", "EventoNota", "{}", 0)});
        responderPublicacionExitosa();

        dispatcher.publicarPendientes();

        ArgumentCaptor<Object> evento = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("exchange.notificaciones"), eq("nota.creada"),
                evento.capture(), any(CorrelationData.class));
        assertThat(evento.getValue()).isInstanceOf(EventoNota.class);
        verify(jdbcTemplate).update(argThat(sql -> sql.contains("ENVIADO")), eq("evt-1"));
    }

    @Test
    void publicaEventosDeEvaluacionYAsistencia() {
        prepararFilas(new String[][] {
                fila("evt-2", "EventoEvaluacion", "{}", 0),
                fila("evt-3", "EventoAsistencia", "{}", 0)});
        responderPublicacionExitosa();

        dispatcher.publicarPendientes();

        verify(rabbitTemplate, org.mockito.Mockito.times(2)).convertAndSend(anyString(), anyString(),
                any(Object.class), any(CorrelationData.class));
        verify(jdbcTemplate).update(argThat(sql -> sql.contains("ENVIADO")), eq("evt-2"));
        verify(jdbcTemplate).update(argThat(sql -> sql.contains("ENVIADO")), eq("evt-3"));
    }

    @Test
    void rechazoDelBrokerRegistraFalloConBackoff() {
        prepararFilas(new String[][] {fila("evt-4", "EventoNota", "{}", 1)});
        responderPublicacion(new CorrelationData.Confirm(false, "cola llena"), null);

        dispatcher.publicarPendientes();

        ArgumentCaptor<Object> proximoIntento = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).update(argThat(sql -> sql.contains("SET estado = ?")),
                eq("PENDIENTE"), eq(2), proximoIntento.capture(),
                argThat(detalle -> detalle.toString().contains("cola llena")), eq("evt-4"));
        LocalDateTime esperado = LocalDateTime.now().plusSeconds(3);
        assertThat((LocalDateTime) proximoIntento.getValue()).isBetween(
                esperado.minusSeconds(2), esperado.plusSeconds(2));
    }

    @Test
    void mensajeSinColaDeDestinoRegistraFallo() {
        prepararFilas(new String[][] {fila("evt-5", "EventoNota", "{}", 0)});
        ReturnedMessage retornado = new ReturnedMessage(new Message(new byte[0]), 312,
                "NO_ROUTE", "exchange.notificaciones", "nota.creada");
        responderPublicacion(new CorrelationData.Confirm(true, null), retornado);

        dispatcher.publicarPendientes();

        verify(jdbcTemplate).update(argThat(sql -> sql.contains("SET estado = ?")),
                eq("PENDIENTE"), eq(1), any(LocalDateTime.class),
                argThat(detalle -> detalle.toString().contains("no encontró una cola")), eq("evt-5"));
    }

    @Test
    void tipoDeEventoDesconocidoRegistraFallo() {
        prepararFilas(new String[][] {fila("evt-6", "EventoRaro", "{}", 0)});

        dispatcher.publicarPendientes();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class),
                any(CorrelationData.class));
        verify(jdbcTemplate).update(argThat(sql -> sql.contains("SET estado = ?")),
                eq("PENDIENTE"), eq(1), any(LocalDateTime.class),
                argThat(detalle -> detalle.toString().contains("desconocido")), eq("evt-6"));
    }

    @Test
    void errorSinMensajeUsaElNombreDeLaExcepcion() throws Exception {
        prepararFilas(new String[][] {fila("evt-7", "EventoNota", "{}", 0)});
        ObjectMapper mapper = mock(ObjectMapper.class);
        when(mapper.readValue(anyString(), eq(EventoNota.class)))
                .thenThrow(new NullPointerException());
        NotificationOutboxDispatcher conMapperRoto =
                new NotificationOutboxDispatcher(jdbcTemplate, mapper, rabbitTemplate);

        conMapperRoto.publicarPendientes();

        verify(jdbcTemplate).update(argThat(sql -> sql.contains("SET estado = ?")),
                eq("PENDIENTE"), eq(1), any(LocalDateTime.class),
                eq("NullPointerException"), eq("evt-7"));
    }

    @Test
    void recortaElDetalleDelErrorA1000Caracteres() throws Exception {
        prepararFilas(new String[][] {fila("evt-8", "EventoNota", "{}", 0)});
        ObjectMapper mapper = mock(ObjectMapper.class);
        when(mapper.readValue(anyString(), eq(EventoNota.class)))
                .thenThrow(new IllegalStateException("x".repeat(1500)));
        NotificationOutboxDispatcher conMapperLargo =
                new NotificationOutboxDispatcher(jdbcTemplate, mapper, rabbitTemplate);

        conMapperLargo.publicarPendientes();

        ArgumentCaptor<Object> detalle = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).update(argThat(sql -> sql.contains("SET estado = ?")),
                eq("PENDIENTE"), eq(1), any(LocalDateTime.class), detalle.capture(), eq("evt-8"));
        assertThat(detalle.getValue().toString()).hasSize(1000);
    }

    @Test
    void alAgotarLosIntentosMarcaFallidoConIntentoInmediato() {
        String[] fila = fila("evt-9", "EventoNota", "{}", 7);
        prepararFilas(new String[][] {fila});
        responderPublicacion(new CorrelationData.Confirm(false, "sin confirmacion"), null);
        LocalDateTime antes = LocalDateTime.now();

        dispatcher.publicarPendientes();

        ArgumentCaptor<Object> proximoIntento = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).update(argThat(sql -> sql.contains("SET estado = ?")),
                eq("FALLIDO"), eq(8), proximoIntento.capture(),
                argThat(detalle -> detalle.toString().contains("sin confirmacion")), eq("evt-9"));
        assertThat((LocalDateTime) proximoIntento.getValue()).isAfterOrEqualTo(antes);
    }

    @Test
    void confirmacionFallidaDelFutureRegistraFallo() {
        prepararFilas(new String[][] {fila("evt-10", "EventoNota", "{}", 0)});
        doAnswer(invocacion -> {
            CorrelationData correlationData = invocacion.getArgument(3);
            correlationData.getFuture().completeExceptionally(new IllegalStateException("timeout"));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class),
                any(CorrelationData.class));

        dispatcher.publicarPendientes();

        verify(jdbcTemplate).update(argThat(sql -> sql.contains("SET estado = ?")),
                eq("PENDIENTE"), eq(1), any(LocalDateTime.class),
                argThat(detalle -> detalle.toString().contains("timeout")), eq("evt-10"));
    }
}
