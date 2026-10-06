package cl.siga.msasistencias.mensajeria;

import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.notificaciones.AccionAsistencia;
import cl.siga.coreshare.dto.notificaciones.EventoAsistencia;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * El Productor de asistencias vive en ms-asistencias.
 * Solo se llama al crear (POST) con estado AUSENTE o ATRASADO; el mensaje
 * lleva el porcentaje de inasistencia del mes y si supera el umbral del 60%.
 * Si RabbitMQ está caído, solo loguea y NO rompe el registro.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PublicadorAsistencia {

    private final RabbitTemplate plantillaConejo;

    /** Se llama al registrar (POST /api/v1/asistencias) si es AUSENTE o ATRASADO. */
    public void publicarRegistrada(AsistenciaResponseDTO asistencia,
                                   double porcentajeInasistencia,
                                   boolean superaUmbral) {
        EventoAsistencia evento = new EventoAsistencia(
                asistencia.id(),
                asistencia.idEstudiante(),
                asistencia.idCursoAsignatura(),
                asistencia.fecha(),
                asistencia.estado(),
                asistencia.justificacion(),
                AccionAsistencia.REGISTRADA,
                porcentajeInasistencia,
                superaUmbral,
                LocalDateTime.now()
        );
        try {
            plantillaConejo.convertAndSend(
                    NombresMensajeria.INTERCAMBIO_NOTIFICACIONES,
                    NombresMensajeria.CLAVE_ASISTENCIA_REGISTRADA,
                    evento
            );
            log.info("Evento de asistencia publicado: id={} estado={} porcentaje={} superaUmbral={}",
                    asistencia.id(), asistencia.estado(), porcentajeInasistencia, superaUmbral);
        } catch (Exception error) {
            // La asistencia ya quedó guardada; la notificación es secundaria.
            log.warn("No se pudo publicar el evento de asistencia id={}: {}",
                    asistencia.id(), error.getMessage());
        }
    }
}
