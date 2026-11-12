package cl.siga.ms_kafka.controller;

import cl.siga.coreshare.dto.asistencia.enums.Justificacion;
import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.ms_kafka.dto.CreateTopicRequest;
import cl.siga.ms_kafka.dto.events.asistencia.AsistenciaEvent;
import cl.siga.ms_kafka.service.AdminService;
import lombok.RequiredArgsConstructor;
import cl.siga.ms_kafka.producer.KafkaProducer;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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


  @GetMapping("/topics/{nombre}")
  public ResponseEntity<?> describirTopic(
    @PathVariable String nombre
  ) throws Exception {

    return ResponseEntity.ok(
      service.describirTopic(nombre)
    );
  }

  @PatchMapping("/topics/{nombre}/retention")
  public ResponseEntity<String> modificarRetencion(
    @PathVariable String nombre,
    @RequestParam Long retentionMs,
    @RequestParam(required = false) Long retentionBytes
  ) throws Exception {

    service.modificarRetencion(
      nombre,
      retentionMs,
      retentionBytes
    );

    return ResponseEntity.ok(
      "Retención actualizada para topic: " + nombre
    );
  }

  // SOLO Para TEST.
  @PostMapping("/test")
  public String test() {

    AsistenciaEvent event =
      new AsistenciaEvent(
        UUID.randomUUID(),
        UUID.randomUUID(),

        1L,
        1L,
        1L,

        LocalDate.now(),

        State.AUSENTE,
        Justificacion.NO,

        "Prueba Kafka",
        "lola"
      );

    kafkaProducer.enviar(event);

    return "Evento enviado";
  }
}
