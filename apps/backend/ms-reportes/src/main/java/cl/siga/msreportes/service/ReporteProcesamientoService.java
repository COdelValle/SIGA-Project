package cl.siga.msreportes.service;

import cl.siga.coreshare.dto.kafka.EventoAsistenciaKafka;
import cl.siga.coreshare.dto.kafka.EventoRendimientoKafka;
import cl.siga.coreshare.dto.notificaciones.NivelRiesgo;
import cl.siga.msreportes.model.entity.*;
import cl.siga.msreportes.repository.*;
import java.time.LocalDateTime;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteProcesamientoService {

    private final Validator validator;
    private final EventoReporteProcesadoRepository eventosRepository;
    private final ProyeccionAsistenciaRepository asistenciaRepository;
    private final ProyeccionRendimientoRepository rendimientoRepository;

    @Transactional
    public void procesarAsistencia(ConsumerRecord<String, EventoAsistenciaKafka> record) {
        EventoAsistenciaKafka evento = validar(record.value());
        if (eventosRepository.existsByIdEvento(evento.idEvento())) {
            log.info("Evento de asistencia duplicado ignorado idEvento={} correlationId={}",
                evento.idEvento(), evento.correlationId());
            return;
        }

        ProyeccionAsistencia proyeccion = asistenciaRepository
            .findByIdEstudianteAndIdCursoAsignatura(
                evento.idEstudiante(), evento.idCursoAsignatura())
            .orElseGet(ProyeccionAsistencia::new);
        proyeccion.setIdEstudiante(evento.idEstudiante());
        proyeccion.setIdCursoAsignatura(evento.idCursoAsignatura());
        proyeccion.setIdAsistencia(evento.idAsistencia());
        proyeccion.setFecha(evento.fecha());
        proyeccion.setEstado(evento.estado());
        proyeccion.setPorcentajeInasistencia(evento.porcentajeInasistencia());
        proyeccion.setTotalRegistros(evento.totalRegistros());
        proyeccion.setTotalInasistencias(evento.totalInasistencias());
        proyeccion.setNivelRiesgo(nivelAsistencia(evento.porcentajeInasistencia()));
        proyeccion.setIdEvento(evento.idEvento());
        proyeccion.setCorrelationId(evento.correlationId());
        proyeccion.setActualizadoEn(evento.timestamp());
        asistenciaRepository.save(proyeccion);
        registrarProcesado(record, evento.idEvento(), evento.timestamp(), evento.correlationId());
    }

    @Transactional
    public void procesarRendimiento(ConsumerRecord<String, EventoRendimientoKafka> record) {
        EventoRendimientoKafka evento = validar(record.value());
        if (eventosRepository.existsByIdEvento(evento.idEvento())) {
            log.info("Evento de rendimiento duplicado ignorado idEvento={} correlationId={}",
                evento.idEvento(), evento.correlationId());
            return;
        }

        ProyeccionRendimiento proyeccion = rendimientoRepository
            .findByIdEstudianteAndIdEvaluacion(evento.idEstudiante(), evento.idEvaluacion())
            .orElseGet(ProyeccionRendimiento::new);
        proyeccion.setIdEstudiante(evento.idEstudiante());
        proyeccion.setIdEvaluacion(evento.idEvaluacion());
        proyeccion.setIdCursoAsignatura(evento.idCursoAsignatura());
        proyeccion.setIdNota(evento.idNota());
        proyeccion.setPuntaje(evento.puntaje());
        proyeccion.setPromedio(evento.promedio());
        proyeccion.setEvaluacionesInsuficientes(evento.evaluacionesInsuficientes());
        proyeccion.setNivelRiesgo(nivelRendimiento(evento));
        proyeccion.setIdEvento(evento.idEvento());
        proyeccion.setCorrelationId(evento.correlationId());
        proyeccion.setActualizadoEn(evento.timestamp());
        rendimientoRepository.save(proyeccion);
        registrarProcesado(record, evento.idEvento(), evento.timestamp(), evento.correlationId());
    }

    private <T> T validar(T evento) {
        if (evento == null) {
            throw new IllegalArgumentException("El payload Kafka no puede ser nulo.");
        }
        var errores = validator.validate(evento);
        if (!errores.isEmpty()) {
            String detalle = errores.stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .sorted()
                .reduce((a, b) -> a + ", " + b)
                .orElse("payload inválido");
            throw new IllegalArgumentException("Payload Kafka inválido: " + detalle);
        }
        return evento;
    }

    private void registrarProcesado(
            ConsumerRecord<?, ?> record,
            String idEvento,
            LocalDateTime timestamp,
            String correlationId) {
        try {
            eventosRepository.save(EventoReporteProcesado.builder()
                .idEvento(idEvento)
                .topic(record.topic())
                .particion(record.partition())
                .offsetKafka(record.offset())
                .correlationId(correlationId)
                .timestampEvento(timestamp)
                .procesadoEn(LocalDateTime.now())
                .build());
        } catch (DataIntegrityViolationException error) {
            throw new IllegalStateException(
                "El evento ya fue registrado concurrentemente: " + idEvento, error);
        }
    }

    private NivelRiesgo nivelAsistencia(double porcentaje) {
        if (porcentaje >= 40.0) {
            return NivelRiesgo.CRITICO;
        }
        if (porcentaje >= 20.0) {
            return NivelRiesgo.PREVENTIVO;
        }
        return NivelRiesgo.NORMAL;
    }

    private NivelRiesgo nivelRendimiento(EventoRendimientoKafka evento) {
        if (evento.evaluacionesInsuficientes() >= 2
                || evento.promedio() != null && evento.promedio() < 4.0) {
            return NivelRiesgo.CRITICO;
        }
        if (evento.promedio() != null && evento.promedio() < 5.0) {
            return NivelRiesgo.PREVENTIVO;
        }
        return NivelRiesgo.NORMAL;
    }
}
