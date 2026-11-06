package cl.siga.ms_kafka.controller;

import cl.siga.ms_kafka.dto.CreateTopicRequest;
import cl.siga.ms_kafka.dto.events.asistencia.AsistenciaEvent;
import cl.siga.ms_kafka.dto.events.notificacion.EstadoAsistencia;
import cl.siga.ms_kafka.dto.events.notificacion.NivelRiesgo;
import cl.siga.ms_kafka.service.AdminService;
import lombok.RequiredArgsConstructor;
import cl.siga.ms_kafka.producer.KafkaProducer;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class AdminController {
  private final KafkaProducer kafkaProducer;

  private final AdminService service;

  @GetMapping("/topics")
  public Set<String> obtenerTopics() throws Exception {

    return service.obtenerTopics();
  }

  @PostMapping("/topics")
  public ResponseEntity<String> crearTopic(
    @RequestBody CreateTopicRequest request)
    throws Exception {

    service.crearTopic(request);

    return ResponseEntity.ok(
      "Topic creado: " + request.nombre()
    );
  }

  @DeleteMapping("/topics/{nombre}")
  public ResponseEntity<String> eliminarTopic(
    @PathVariable String nombre)
    throws Exception {

    service.eliminarTopic(nombre);

    return ResponseEntity.ok(
      "Topic eliminado: " + nombre
    );
  }

  @PostMapping("/test")
  public String test() {

    AsistenciaEvent event =
      new AsistenciaEvent(
        UUID.randomUUID(),
        UUID.randomUUID(),
        1L,
        1L,
        1L,
        LocalDateTime.now(),
        EstadoAsistencia.JUSTIFICADO,
        NivelRiesgo.ALTO,
        "lola",
        60
      );

    kafkaProducer.enviar(event);
    return "Evento enviado";
  }
}
