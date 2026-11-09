package cl.siga.msreportes.service;

import cl.siga.coreshare.dto.kafka.EventoAsistenciaKafka;
import cl.siga.coreshare.dto.kafka.EventoRendimientoKafka;
import cl.siga.coreshare.dto.notificaciones.NivelRiesgo;
import cl.siga.coreshare.dto.notificaciones.TipoRiesgo;
import cl.siga.msreportes.model.entity.*;
import cl.siga.msreportes.repository.*;
import cl.siga.msreportes.mensajeria.PublicadorAlertaRiesgo;
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
    private final AlertaRiesgoRepository alertasRepository;
    private final PublicadorAlertaRiesgo publicadorAlerta;

    public ReporteProcesamientoService(
            Validator validator,
            EventoReporteProcesadoRepository eventosRepository,
            ProyeccionAsistenciaRepository asistenciaRepository,
            ProyeccionRendimientoRepository rendimientoRepository,
            AlertaRiesgoRepository alertasRepository) {
        this(validator, eventosRepository, asistenciaRepository, rendimientoRepository,
            alertasRepository, null);
    }

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
        NivelRiesgo nivelAnterior = proyeccion.getNivelRiesgo();
        double porcentajeAnterior = proyeccion.getPorcentajeInasistencia();
        NivelRiesgo nivelNuevo = nivelAsistencia(evento.porcentajeInasistencia());
        proyeccion.setIdEstudiante(evento.idEstudiante());
        proyeccion.setIdCursoAsignatura(evento.idCursoAsignatura());
        proyeccion.setIdAsistencia(evento.idAsistencia());
        proyeccion.setFecha(evento.fecha());
        proyeccion.setEstado(evento.estado());
        proyeccion.setPorcentajeInasistencia(evento.porcentajeInasistencia());
        proyeccion.setTotalRegistros(evento.totalRegistros());
        proyeccion.setTotalInasistencias(evento.totalInasistencias());
        proyeccion.setNivelRiesgo(nivelNuevo);
        proyeccion.setIdEvento(evento.idEvento());
        proyeccion.setCorrelationId(evento.correlationId());
        proyeccion.setActualizadoEn(evento.timestamp());
        asistenciaRepository.save(proyeccion);
        registrarAlertaAsistencia(evento, nivelAnterior, nivelNuevo, porcentajeAnterior);
        registrarCambioEstado(evento.idEvento(), evento.correlationId(), evento.idEstudiante(),
            evento.idCursoAsignatura(), nivelAnterior, nivelNuevo, "asistencia");
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
        NivelRiesgo nivelAnterior = proyeccion.getNivelRiesgo();
        NivelRiesgo nivelNuevo = nivelRendimiento(evento);
        proyeccion.setIdEstudiante(evento.idEstudiante());
        proyeccion.setIdEvaluacion(evento.idEvaluacion());
        proyeccion.setIdCursoAsignatura(evento.idCursoAsignatura());
        proyeccion.setIdNota(evento.idNota());
        proyeccion.setPuntaje(evento.puntaje());
        proyeccion.setPromedio(evento.promedio());
        proyeccion.setEvaluacionesInsuficientes(evento.evaluacionesInsuficientes());
        proyeccion.setNivelRiesgo(nivelNuevo);
        proyeccion.setIdEvento(evento.idEvento());
        proyeccion.setCorrelationId(evento.correlationId());
        proyeccion.setActualizadoEn(evento.timestamp());
        rendimientoRepository.save(proyeccion);
        registrarAlertaRendimiento(evento, nivelAnterior, nivelNuevo);
        registrarCambioEstado(evento.idEvento(), evento.correlationId(), evento.idEstudiante(),
            evento.idCursoAsignatura(), nivelAnterior, nivelNuevo, "rendimiento");
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
        if (porcentaje > 40.0) {
            return NivelRiesgo.CRITICO;
        }
        if (porcentaje > 20.0) {
            return NivelRiesgo.PREVENTIVO;
        }
        return NivelRiesgo.NORMAL;
    }

    private NivelRiesgo nivelRendimiento(EventoRendimientoKafka evento) {
        Double indicador = evento.promedio() == null ? evento.puntaje() : evento.promedio();
        if (evento.evaluacionesInsuficientes() >= 2
                || indicador != null && indicador < 4.0) {
            return NivelRiesgo.CRITICO;
        }
        if (indicador != null && indicador < 5.0) {
            return NivelRiesgo.PREVENTIVO;
        }
        return NivelRiesgo.NORMAL;
    }

    private void registrarAlertaAsistencia(
            EventoAsistenciaKafka evento,
            NivelRiesgo nivelAnterior,
            NivelRiesgo nivelNuevo,
            double porcentajeAnterior) {
        int umbral = umbralCruzado(porcentajeAnterior, evento.porcentajeInasistencia());
        boolean cambioEstado = nivelNuevo != nivelAnterior
            && nivelNuevo != NivelRiesgo.NORMAL;
        if (umbral == 0 && !cambioEstado) {
            return;
        }
        String clave = "ASISTENCIA|" + evento.idEvento() + "|" + umbral + "|" + nivelNuevo;
        registrarAlerta(AlertaRiesgo.builder()
            .claveIdempotencia(clave)
            .tipoRiesgo(TipoRiesgo.INASISTENCIA)
            .idEstudiante(evento.idEstudiante())
            .idCursoAsignatura(evento.idCursoAsignatura())
            .nivelAnterior(nivelAnterior)
            .nivelNuevo(nivelNuevo)
            .porcentajeInasistencia(evento.porcentajeInasistencia())
            .motivo(motivoAsistencia(evento.porcentajeInasistencia(), umbral))
            .idEvento(evento.idEvento())
            .correlationId(evento.correlationId())
            .creadaEn(LocalDateTime.now())
            .build());
    }

    private void registrarAlertaRendimiento(
            EventoRendimientoKafka evento,
            NivelRiesgo nivelAnterior,
            NivelRiesgo nivelNuevo) {
        if (nivelNuevo == NivelRiesgo.NORMAL
                || nivelNuevo == nivelAnterior && nivelAnterior != null) {
            return;
        }
        Double indicador = evento.promedio() == null ? evento.puntaje() : evento.promedio();
        String motivo = evento.evaluacionesInsuficientes() >= 2
            ? "El estudiante acumula dos o más evaluaciones insuficientes."
            : "El indicador de rendimiento es inferior al mínimo establecido: " + indicador + ".";
        registrarAlerta(AlertaRiesgo.builder()
            .claveIdempotencia("RENDIMIENTO|" + evento.idEvento() + "|" + nivelNuevo)
            .tipoRiesgo(TipoRiesgo.BAJO_RENDIMIENTO)
            .idEstudiante(evento.idEstudiante())
            .idCursoAsignatura(evento.idCursoAsignatura())
            .nivelAnterior(nivelAnterior)
            .nivelNuevo(nivelNuevo)
            .promedio(evento.promedio() == null ? evento.puntaje() : evento.promedio())
            .evaluacionesInsuficientes(evento.evaluacionesInsuficientes())
            .motivo(motivo)
            .idEvento(evento.idEvento())
            .correlationId(evento.correlationId())
            .creadaEn(LocalDateTime.now())
            .build());
    }

    private void registrarCambioEstado(
            String idEvento,
            String correlationId,
            Long idEstudiante,
            Long idCursoAsignatura,
            NivelRiesgo nivelAnterior,
            NivelRiesgo nivelNuevo,
            String dominio) {
        if (nivelAnterior == null || nivelAnterior == nivelNuevo) {
            return;
        }
        registrarAlerta(AlertaRiesgo.builder()
            .claveIdempotencia("ESTADO|" + dominio + "|" + idEvento + "|" + nivelNuevo)
            .tipoRiesgo(TipoRiesgo.CAMBIO_ESTADO_ACADEMICO)
            .idEstudiante(idEstudiante)
            .idCursoAsignatura(idCursoAsignatura)
            .nivelAnterior(nivelAnterior)
            .nivelNuevo(nivelNuevo)
            .motivo("El estado académico cambió de " + nivelAnterior + " a " + nivelNuevo + ".")
            .idEvento(idEvento)
            .correlationId(correlationId)
            .creadaEn(LocalDateTime.now())
            .build());
    }

    private void registrarAlerta(AlertaRiesgo alerta) {
        if (alertasRepository.existsByClaveIdempotencia(alerta.getClaveIdempotencia())) {
            log.info("Alerta duplicada ignorada claveIdempotencia={}", alerta.getClaveIdempotencia());
            return;
        }
        try {
            alertasRepository.save(alerta);
            if (publicadorAlerta != null) {
                publicadorAlerta.publicar(alerta);
            }
        } catch (DataIntegrityViolationException error) {
            log.info("Alerta concurrente ignorada claveIdempotencia={}", alerta.getClaveIdempotencia());
        }
    }

    private int umbralCruzado(double anterior, double actual) {
        if (anterior == 0.0 && actual > 60.0) {
            return 60;
        }
        if (anterior == 0.0 && actual > 40.0) {
            return 40;
        }
        if (anterior == 0.0 && actual > 20.0) {
            return 20;
        }
        if (anterior <= 60.0 && actual > 60.0) {
            return 60;
        }
        if (anterior <= 40.0 && actual > 40.0) {
            return 40;
        }
        if (anterior <= 20.0 && actual > 20.0) {
            return 20;
        }
        return 0;
    }

    private String motivoAsistencia(double porcentaje, int umbral) {
        if (umbral > 0) {
            return "La inasistencia superó el umbral del " + umbral + "% (" + porcentaje + "%).";
        }
        return "El nivel de riesgo de inasistencia cambió a preventivo o crítico.";
    }
}
