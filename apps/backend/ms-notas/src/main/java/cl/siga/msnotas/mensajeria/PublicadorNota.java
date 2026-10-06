package cl.siga.msnotas.mensajeria;

import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.dto.notificaciones.AccionNota;
import cl.siga.coreshare.dto.notificaciones.EventoNota;
import cl.siga.coreshare.mensajeria.NombresMensajeria;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * El Productor de notas vive en ms-notas.
 * Se llama al crear (POST, fila nueva o reactivación) y al modificar (PUT).
 * Si RabbitMQ está caído, solo loguea y NO rompe el registro.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PublicadorNota {

    private final RabbitTemplate plantillaConejo;

    /** POST con fila nueva. */
    public void publicarCreada(NotaResponseDTO nota) {
        publicar(nota, AccionNota.CREADA, NombresMensajeria.CLAVE_NOTA_CREADA);
    }

    /** PUT, o POST que reactiva una nota borrada lógicamente. */
    public void publicarActualizada(NotaResponseDTO nota) {
        publicar(nota, AccionNota.ACTUALIZADA, NombresMensajeria.CLAVE_NOTA_ACTUALIZADA);
    }

    private void publicar(NotaResponseDTO nota, AccionNota accion, String claveEnrutamiento) {
        EventoNota evento = new EventoNota(
                nota.id(),
                nota.idEstudiante(),
                nota.idEvaluacion(),
                nota.score(),
                accion,
                LocalDateTime.now()
        );
        try {
            plantillaConejo.convertAndSend(
                    NombresMensajeria.INTERCAMBIO_NOTIFICACIONES,
                    claveEnrutamiento,
                    evento
            );
            log.info("Evento de nota publicado: accion={} id={} clave={}",
                    accion, nota.id(), claveEnrutamiento);
        } catch (Exception error) {
            // La nota ya quedó guardada; la notificación es secundaria.
            log.warn("No se pudo publicar el evento de nota id={} accion={}: {}",
                    nota.id(), accion, error.getMessage());
        }
    }
}
