package cl.siga.ms_kafka.controller;

import cl.siga.ms_kafka.dto.CreateTopicRequest;
import cl.siga.ms_kafka.service.AdminService;
import lombok.RequiredArgsConstructor;
import cl.siga.ms_kafka.producer.KafkaProducer;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class AdminController {
  //private final KafkaProducer kafkaProducer;

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

  @GetMapping("/consumer-groups")
  public ResponseEntity<?> obtenerConsumerGroups()
    throws Exception {

    return ResponseEntity.ok(
      service.obtenerConsumerGroups()
    );
  }

  @GetMapping("/consumer-groups/{groupId}")
  public ResponseEntity<?> describirConsumerGroup(
    @PathVariable String groupId
  ) throws Exception {

    return ResponseEntity.ok(
      service.describirConsumerGroup(groupId)
    );
  }

  @GetMapping("/consumer-groups/{groupId}/lag")
  public ResponseEntity<?> obtenerConsumerLag(
    @PathVariable String groupId
  ) throws Exception {

    return ResponseEntity.ok(
      service.obtenerConsumerLag(groupId)
    );
  }

  @GetMapping("/brokers")
  public ResponseEntity<?> obtenerBrokers() {

    try {
      return ResponseEntity.ok(service.obtenerBrokers());
    } catch (Exception e) {

      Map<String, Object> error = new HashMap<>();

      error.put("error", "No se pudieron consultar los brokers");
      error.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );
      return ResponseEntity.internalServerError().body(error);
    }
  }

  @GetMapping("/cluster/status")
  public ResponseEntity<Map<String, Object>> obtenerEstadoCluster() {

    Map<String, Object> respuesta = service.obtenerEstadoCluster();

    if ("ERROR".equals(respuesta.get("estado"))) {
      return ResponseEntity.internalServerError().body(respuesta);
    }

    return ResponseEntity.ok(respuesta);
  }
}
