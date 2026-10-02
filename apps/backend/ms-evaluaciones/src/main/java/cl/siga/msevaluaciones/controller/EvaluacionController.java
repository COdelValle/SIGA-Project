package cl.siga.msevaluaciones.controller;

import cl.siga.coreshare.dto.evaluaciones.ActualizarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.RegistrarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import cl.siga.msevaluaciones.service.EvaluacionService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/v1/evaluaciones")
@RequiredArgsConstructor
@Tag(name = "Evaluaciones", description = "Operaciones de registro, modificación y consulta de evaluaciones")
public class EvaluacionController {

  private final EvaluacionService evaluacionService;

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('SCOPE_evaluaciones:read')")
  public ResponseEntity<EvaluacionResponseDTO> getEvaluacionById(@PathVariable Long id) {
    return ResponseEntity.ok(evaluacionService.getEvaluacionById(id));
  }

  @GetMapping("/search")
  @PreAuthorize("hasAuthority('SCOPE_evaluaciones:read')")
  public ResponseEntity<Page<EvaluacionResponseDTO>> searchEvaluaciones(
    @RequestParam(required = false) String nombre,
    @RequestParam(required = false) TipoEvaluacion tipo,
    @RequestParam(required = false) Long idAsignatura,
    @PageableDefault(size = 20, sort = "id") Pageable pageable
  ) {
    return ResponseEntity.ok(evaluacionService.searchEvaluaciones(nombre, tipo, idAsignatura, pageable));
  }

  @PostMapping
  @PreAuthorize("(hasRole('ADMIN') or (hasRole('DOCENTE')) and hasAuthority('SCOPE_evaluaciones:write'))")
  public ResponseEntity<EvaluacionResponseDTO> registrarEvaluacion(@RequestBody @Valid RegistrarEvaluacionRequestDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(evaluacionService.saveEvaluacion(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("(hasRole('ADMIN') or (hasRole('DOCENTE')) and hasAuthority('SCOPE_evaluaciones:update'))")
  public ResponseEntity<EvaluacionResponseDTO> updateEvaluacion(
    @PathVariable Long id,
    @RequestBody @Valid ActualizarEvaluacionRequestDTO request
  ) {
    return ResponseEntity.ok(evaluacionService.updateEvaluacion(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("(hasRole('ADMIN') or (hasRole('DOCENTE')) and hasAuthority('SCOPE_evaluaciones:delete'))")
  public ResponseEntity<Void> deleteEvaluacion(@PathVariable Long id) {
    evaluacionService.deleteEvaluacion(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/exists/{id}")
  @PreAuthorize("hasAuthority('SCOPE_evaluaciones:read')")
  public ResponseEntity<Boolean> existsEvaluacionById(@PathVariable Long id) {
    return ResponseEntity.ok(evaluacionService.existsEvaluacionById(id));
  }
}
