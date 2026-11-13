package cl.siga.ms_kafka.controller;

import cl.siga.ms_kafka.dto.CreateTopicRequest;
import cl.siga.ms_kafka.dto.ResetConsumerGroupOffsetsRequest;
import cl.siga.ms_kafka.service.AdminService;
import lombok.RequiredArgsConstructor;
//import cl.siga.ms_kafka.producer.KafkaProducer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class AdminController {
  //private final KafkaProducer kafkaProducer;

  private final AdminService service;

  //TOPICS
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

  @GetMapping("/topics/{nombre}/details")
  public ResponseEntity<?> obtenerDetallesTopic(
    @PathVariable String nombre
  ) throws Exception {

    return ResponseEntity.ok(
      service.obtenerDetallesTopic(nombre)
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

  @GetMapping("/topics/health")
  public ResponseEntity<?> obtenerEstadoParticiones() {

    try {
      return ResponseEntity.ok(
        service.obtenerEstadoParticiones()
      );

    } catch (Exception e) {

      Map<String, Object> error = new HashMap<>();

      error.put("error", "No se pudo consultar el estado de las particiones");
      error.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError().body(error);
    }
  }

  @GetMapping("/topics/{nombre}/config")
  public ResponseEntity<?> obtenerConfiguracionTopic(
    @PathVariable String nombre
  ) throws Exception {

    return ResponseEntity.ok(
      service.obtenerConfiguracionTopic(nombre)
    );
  }

  @PatchMapping("/topics/{nombre}/config")
  public ResponseEntity<?> modificarConfiguracionTopic(
    @PathVariable String nombre,
    @RequestBody Map<String, String> propiedades
  ) throws Exception {

    service.modificarConfiguracionTopic(
      nombre,
      propiedades
    );

    Map<String, Object> respuesta = new LinkedHashMap<>();

    respuesta.put("mensaje", "Configuración actualizada correctamente.");
    respuesta.put("topic", nombre);
    respuesta.put("configuracionesActualizadas", propiedades);

    return ResponseEntity.ok(respuesta);
  }

  @PatchMapping("/topics/{nombre}/config/reset")
  public ResponseEntity<?> restaurarConfiguracionTopic(
    @PathVariable String nombre,
    @RequestBody List<String> propiedades
  ) throws Exception {

    service.restaurarConfiguracionTopic(nombre, propiedades);

    Map<String, Object> respuesta = new LinkedHashMap<>();

    respuesta.put(
      "mensaje",
      "Configuraciones restauradas correctamente."
    );

    respuesta.put("topic", nombre);
    respuesta.put("configuracionesRestauradas", propiedades);

    return ResponseEntity.ok(respuesta);
  }
  //END-TOPICS

  //CONSUMER-GROUPS
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
    @PathVariable String groupId) {

    try {

      return ResponseEntity.ok(
        service.obtenerConsumerLag(groupId)
      );

    } catch (NoSuchElementException e) {

      Map<String, Object> error = new HashMap<>();

      error.put("error", "Consumer Group no encontrado");
      error.put("detalle", e.getMessage());

      return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(error);

    } catch (Exception e) {

      Map<String, Object> error = new HashMap<>();

      error.put("error", "No se pudo consultar el Consumer Lag");
      error.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError()
        .body(error);
    }
  }
  //BROKERS
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

  @GetMapping("/brokers/distribution")
  public ResponseEntity<?> obtenerDistribucionBrokers() {

    try {
      return ResponseEntity.ok(
        service.obtenerDistribucionBrokers()
      );

    } catch (Exception e) {

      Map<String, Object> error = new HashMap<>();

      error.put("error", "No se pudo consultar la distribución de brokers");
      error.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError().body(error);
    }
  }
  //CLUSTER
  @GetMapping("/cluster/status")
  public ResponseEntity<Map<String, Object>> obtenerEstadoCluster() {

    Map<String, Object> respuesta = service.obtenerEstadoCluster();

    if ("ERROR".equals(respuesta.get("estado"))) {
      return ResponseEntity.internalServerError().body(respuesta);
    }

    return ResponseEntity.ok(respuesta);
  }

  //OFFSETs
  @GetMapping("/topics/{nombre}/offsets")
  public ResponseEntity<?> obtenerOffsetsTopic(
    @PathVariable String nombre
  ) throws Exception {

    return ResponseEntity.ok(
      service.obtenerOffsetsTopic(nombre)
    );
  }

  @GetMapping("/consumer-groups/{groupId}/offsets")
  public ResponseEntity<?> obtenerOffsetsConsumerGroup(
    @PathVariable String groupId
  ) throws Exception {

    return ResponseEntity.ok(
      service.obtenerOffsetsConsumerGroup(groupId)
    );
  }

  @DeleteMapping("/consumer-groups/{groupId}/offsets")
  public ResponseEntity<?> eliminarOffsetsConsumerGroup(
    @PathVariable String groupId,
    @RequestParam(defaultValue = "false") boolean confirmar
  ) throws Exception {

    if (!confirmar) {
      return ResponseEntity.badRequest().body(
        Map.of(
          "error",
          "Debe enviar confirmar=true para eliminar los offsets."
        )
      );
    }

    return ResponseEntity.ok(
      service.eliminarOffsetsConsumerGroup(groupId)
    );
  }

  @PatchMapping("/consumer-groups/{groupId}/offsets/reset")
  public ResponseEntity<?> reiniciarOffsetsConsumerGroup(
    @PathVariable String groupId,
    @RequestBody ResetConsumerGroupOffsetsRequest request
  ) throws Exception {

    try {
      return ResponseEntity.ok(
        service.reiniciarOffsetsConsumerGroup(groupId, request)
      );

    } catch (IllegalArgumentException e) {

      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", e.getMessage());

      return ResponseEntity.badRequest().body(respuesta);

    } catch (IllegalStateException e) {

      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", e.getMessage());

      return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(respuesta);
    }
  }

}
