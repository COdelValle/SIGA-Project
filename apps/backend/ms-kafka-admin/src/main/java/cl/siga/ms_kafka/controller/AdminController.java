package cl.siga.ms_kafka.controller;

import cl.siga.ms_kafka.dto.CreateTopicRequest;
import cl.siga.ms_kafka.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class AdminController {

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
      "Topic creado correctamente"
    );
  }
}
