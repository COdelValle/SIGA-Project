package cl.siga.ms_kafka.controller;

import cl.siga.ms_kafka.dto.CreateTopicRequest;
import cl.siga.ms_kafka.dto.ModificarRetencionRequest;
import cl.siga.ms_kafka.dto.ResetConsumerGroupOffsetsRequest;
import cl.siga.ms_kafka.service.AdminService;
import lombok.RequiredArgsConstructor;
//import cl.siga.ms_kafka.producer.KafkaProducer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import cl.siga.ms_kafka.dto.ModificarRetencionRequest;
import java.util.*;

@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class AdminController {
  //private final KafkaProducer kafkaProducer;

  private final AdminService service;

  //TOPICS
  @GetMapping("/topics")
  public ResponseEntity<?> obtenerTopics() {

    try {
      return ResponseEntity.ok(
        service.obtenerTopics()
      );

    } catch (Exception e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "No se pudieron consultar los Topics");
      respuesta.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError().body(respuesta);
    }
  }

  @PostMapping("/topics")
  public ResponseEntity<?> crearTopic(
    @RequestBody CreateTopicRequest request
  ) {

    try {
      service.crearTopic(request);

      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("mensaje", "Topic creado correctamente");
      respuesta.put("topic", request.nombre());

      return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);

    } catch (IllegalArgumentException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", e.getMessage());

      return ResponseEntity.badRequest().body(respuesta);

    } catch (org.apache.kafka.common.errors.TopicExistsException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "El Topic ya existe");
      respuesta.put("detalle", e.getMessage());

      return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);

    } catch (Exception e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "No se pudo crear el Topic");
      respuesta.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError().body(respuesta);
    }
  }

  @DeleteMapping("/topics/{nombre}")
  public ResponseEntity<?> eliminarTopic(
    @PathVariable String nombre
  ) {

    try {
      service.eliminarTopic(nombre);

      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("mensaje", "Topic eliminado correctamente");
      respuesta.put("topic", nombre);

      return ResponseEntity.ok(respuesta);

    } catch (IllegalArgumentException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", e.getMessage());

      return ResponseEntity.badRequest().body(respuesta);

    } catch (NoSuchElementException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "Topic no encontrado");
      respuesta.put("detalle", e.getMessage());

      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);

    } catch (Exception e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "No se pudo eliminar el Topic");
      respuesta.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError().body(respuesta);
    }
  }


  @GetMapping("/topics/{nombre}")
  public ResponseEntity<?> describirTopic(
    @PathVariable String nombre
  ) {

    try {
      return ResponseEntity.ok(
        service.describirTopic(nombre)
      );

    } catch (IllegalArgumentException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", e.getMessage());

      return ResponseEntity.badRequest().body(respuesta);

    } catch (NoSuchElementException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "Topic no encontrado");
      respuesta.put("detalle", e.getMessage());

      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);

    } catch (Exception e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "No se pudo consultar el Topic");
      respuesta.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError().body(respuesta);
    }
  }

  @GetMapping("/topics/{nombre}/details")
  public ResponseEntity<?> obtenerDetallesTopic(
    @PathVariable String nombre
  ) {

    try {
      return ResponseEntity.ok(
        service.obtenerDetallesTopic(nombre)
      );

    } catch (IllegalArgumentException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", e.getMessage());

      return ResponseEntity.badRequest().body(respuesta);

    } catch (NoSuchElementException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "Topic no encontrado");
      respuesta.put("detalle", e.getMessage());

      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);

    } catch (Exception e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "No se pudieron consultar los detalles del Topic");
      respuesta.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError().body(respuesta);
    }
  }
  @PatchMapping("/topics/{nombre}/retention")
  public ResponseEntity<?> modificarRetencion(
    @PathVariable String nombre,
    @RequestBody ModificarRetencionRequest request
  ) {

    try {
      if (request == null || request.retentionMs() == null) {
        return ResponseEntity.badRequest().body(
          Map.of("error", "Debe proporcionar el parámetro retentionMs.")
        );
      }

      service.modificarRetencion(
        nombre,
        request.retentionMs(),
        request.retentionBytes()
      );

      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("mensaje", "Retención actualizada correctamente.");
      respuesta.put("topic", nombre);
      respuesta.put("retentionMs", request.retentionMs());
      respuesta.put("retentionBytes", request.retentionBytes());

      return ResponseEntity.ok(respuesta);

    } catch (IllegalArgumentException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", e.getMessage());

      return ResponseEntity.badRequest().body(respuesta);

    } catch (NoSuchElementException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "Topic no encontrado");
      respuesta.put("detalle", e.getMessage());

      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);

    } catch (Exception e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "No se pudo modificar la retención del Topic");
      respuesta.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError().body(respuesta);
    }
  }

  @GetMapping("/topics/health")
  public ResponseEntity<Map<String, Object>> obtenerEstadoParticiones() {

    try {
      return ResponseEntity.ok(
        service.obtenerEstadoParticiones()
      );

    } catch (Exception e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put(
        "error",
        "No se pudo consultar el estado de las particiones"
      );
      respuesta.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError().body(respuesta);
    }
  }


  @GetMapping("/topics/{nombre}/config")
  public ResponseEntity<?> obtenerConfiguracionTopic(
    @PathVariable String nombre
  ) {

    try {
      return ResponseEntity.ok(
        service.obtenerConfiguracionTopic(nombre)
      );

    } catch (IllegalArgumentException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", e.getMessage());

      return ResponseEntity.badRequest().body(respuesta);

    } catch (NoSuchElementException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "Topic no encontrado");
      respuesta.put("detalle", e.getMessage());

      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);

    } catch (Exception e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "No se pudo consultar la configuración del Topic");
      respuesta.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError().body(respuesta);
    }
  }

  @PatchMapping("/topics/{nombre}/config")
  public ResponseEntity<?> modificarConfiguracionTopic(
    @PathVariable String nombre,
    @RequestBody Map<String, String> propiedades
  ) {

    try {
      service.modificarConfiguracionTopic(nombre, propiedades);

      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("mensaje", "Configuración actualizada correctamente.");
      respuesta.put("topic", nombre);
      respuesta.put("configuracionesActualizadas", propiedades);

      return ResponseEntity.ok(respuesta);

    } catch (IllegalArgumentException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", e.getMessage());

      return ResponseEntity.badRequest().body(respuesta);

    } catch (NoSuchElementException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "Topic no encontrado");
      respuesta.put("detalle", e.getMessage());

      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);

    } catch (Exception e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "No se pudo modificar la configuración del Topic");
      respuesta.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError().body(respuesta);
    }
  }

  @PatchMapping("/topics/{nombre}/config/reset")
  public ResponseEntity<?> restaurarConfiguracionTopic(
    @PathVariable String nombre,
    @RequestBody List<String> propiedades
  ) {

    try {
      service.restaurarConfiguracionTopic(nombre, propiedades);

      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("mensaje", "Configuraciones restauradas correctamente.");
      respuesta.put("topic", nombre);
      respuesta.put("configuracionesRestauradas", propiedades);

      return ResponseEntity.ok(respuesta);

    } catch (IllegalArgumentException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", e.getMessage());

      return ResponseEntity.badRequest().body(respuesta);

    } catch (NoSuchElementException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "Topic no encontrado");
      respuesta.put("detalle", e.getMessage());

      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);

    } catch (Exception e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "No se pudieron restaurar las configuraciones del Topic");
      respuesta.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError().body(respuesta);
    }
  }
  //END-TOPICS

  //CONSUMER-GROUPS
  @GetMapping("/consumer-groups")
  public ResponseEntity<?> obtenerConsumerGroups() {

    try {
      return ResponseEntity.ok(
        service.obtenerConsumerGroups()
      );

    } catch (Exception e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put(
        "error",
        "No se pudieron consultar los Consumer Groups"
      );
      respuesta.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError().body(respuesta);
    }
  }

  @GetMapping("/consumer-groups/{groupId}")
  public ResponseEntity<?> describirConsumerGroup(
    @PathVariable String groupId
  ) {

    try {
      return ResponseEntity.ok(
        service.describirConsumerGroup(groupId)
      );

    } catch (NoSuchElementException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "Consumer Group no encontrado");
      respuesta.put("detalle", e.getMessage());

      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);

    } catch (IllegalArgumentException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", e.getMessage());

      return ResponseEntity.badRequest().body(respuesta);

    } catch (Exception e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "No se pudo consultar el Consumer Group");
      respuesta.put("detalle", Objects.toString(
        e.getMessage(), "Error desconocido"
      ));

      return ResponseEntity.internalServerError().body(respuesta);
    }
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
      return ResponseEntity.ok(
        service.obtenerBrokers()
      );

    } catch (Exception e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "No se pudieron consultar los brokers");
      respuesta.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError().body(respuesta);
    }
  }

  @GetMapping("/brokers/distribution")
  public ResponseEntity<?> obtenerDistribucionBrokers() {

    try {
      return ResponseEntity.ok(
        service.obtenerDistribucionBrokers()
      );

    } catch (Exception e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put(
        "error",
        "No se pudo consultar la distribución de brokers"
      );
      respuesta.put(
        "detalle",
        Objects.toString(e.getMessage(), "Error desconocido")
      );

      return ResponseEntity.internalServerError().body(respuesta);
    }
  }
  //CLUSTER
  @GetMapping("/cluster/status")
  public ResponseEntity<Map<String, Object>> obtenerEstadoCluster() {

    Map<String, Object> respuesta = service.obtenerEstadoCluster();

    if (respuesta == null) {
      respuesta = new LinkedHashMap<>();
      respuesta.put("estado", "ERROR");
      respuesta.put("detalle", "No se recibió respuesta del servicio Kafka.");
    }

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
  ) {

    try {
      return ResponseEntity.ok(
        service.obtenerOffsetsConsumerGroup(groupId)
      );

    } catch (NoSuchElementException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "Consumer Group no encontrado");
      respuesta.put("detalle", e.getMessage());

      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);

    } catch (IllegalArgumentException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", e.getMessage());

      return ResponseEntity.badRequest().body(respuesta);

    } catch (Exception e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "No se pudieron consultar los offsets");
      respuesta.put("detalle", Objects.toString(
        e.getMessage(), "Error desconocido"
      ));

      return ResponseEntity.internalServerError().body(respuesta);
    }
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

    try {
      return ResponseEntity.ok(
        service.eliminarOffsetsConsumerGroup(groupId)
      );

    } catch (NoSuchElementException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "Consumer Group no encontrado");
      respuesta.put("detalle", e.getMessage());

      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);

    } catch (IllegalStateException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", e.getMessage());

      return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
    }
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

    } catch (NoSuchElementException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", "Consumer Group no encontrado");
      respuesta.put("detalle", e.getMessage());

      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);

    } catch (IllegalArgumentException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", e.getMessage());

      return ResponseEntity.badRequest().body(respuesta);

    } catch (IllegalStateException e) {
      Map<String, Object> respuesta = new LinkedHashMap<>();
      respuesta.put("error", e.getMessage());

      return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
    }
  }
}
