package cl.siga.msapoderados.controller;

import cl.siga.coreshare.dto.apoderado.ActualizarApoderadoRequestDTO;
import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.apoderado.RegistrarAdoderadoRequestDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.msapoderados.service.ApoderadoService;
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
@RequestMapping("/api/v1/apoderados")
@RequiredArgsConstructor
@Tag(name = "Apoderados", description = "Operaciones CRUD, búsqueda y vinculación de apoderados y sus pupilos")
public class ApoderadoController {

  private final ApoderadoService apoderadoService;

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('SCOPE_apoderados:read')")
  public ResponseEntity<ApoderadoResponseDTO> getApoderadoById(@PathVariable Long id) {
    return ResponseEntity.ok(apoderadoService.getApoderadoById(id));
  }

  @GetMapping("/idUsuario/{idUsuario}")
  @PreAuthorize("hasAuthority('SCOPE_apoderados:read')")
  public ResponseEntity<ApoderadoResponseDTO> getApoderadoByIdUsuario(@PathVariable String idUsuario) {
    return ResponseEntity.ok(apoderadoService.getApoderadoByIdUsuario(idUsuario));
  }

  @GetMapping("/search")
  @PreAuthorize("hasAuthority('SCOPE_apoderados:read')")
  public ResponseEntity<Page<ApoderadoResponseDTO>> searchApoderados(
    @RequestParam(required = false) String rut,
    @RequestParam(required = false) String firstName,
    @RequestParam(required = false) String firstSurname,
    @RequestParam(required = false) Long idEstudiante,
    @PageableDefault(size = 20, sort = "id") Pageable pageable
  ) {
    return ResponseEntity.ok(apoderadoService.searchApoderados(rut, firstName, firstSurname, idEstudiante, pageable));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_apoderados:write')")
  public ResponseEntity<ApoderadoResponseDTO> registrarApoderado(@RequestBody @Valid RegistrarAdoderadoRequestDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(apoderadoService.saveApoderado(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("(hasRole('ADMIN') or hasRole('APODERADO')) and hasAuthority('SCOPE_apoderados:update')")
  public ResponseEntity<ApoderadoResponseDTO> actualizarApoderado(
    @PathVariable Long id,
    @RequestBody @Valid ActualizarApoderadoRequestDTO request) {
    return ResponseEntity.ok(apoderadoService.updateApoderado(id, request));
  }

  // --- Endpoints para la gestión aislada de estudiantes ---

  @PostMapping("/{id}/estudiantes")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_apoderados:update')")
  public ResponseEntity<ApoderadoResponseDTO> addEstudiante(
    @PathVariable Long id,
    @RequestBody @Valid ParentescoEstudianteDTO request) {
    return ResponseEntity.ok(apoderadoService.addEstudiante(id, request));
  }

  @DeleteMapping("/{id}/estudiantes/{idEstudiante}")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_apoderados:update')")
  public ResponseEntity<ApoderadoResponseDTO> removeEstudiante(
    @PathVariable Long id,
    @PathVariable Long idEstudiante) {
    return ResponseEntity.ok(apoderadoService.removeEstudiante(id, idEstudiante));
  }

  // --------------------------------------------------------

  @GetMapping("/exists/{id}")
  @PreAuthorize("hasAuthority('SCOPE_apoderados:read')")
  public ResponseEntity<Boolean> existsApoderadoById(@PathVariable Long id) {
    return ResponseEntity.ok(apoderadoService.existsApoderadoById(id));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_apoderados:delete')")
  public ResponseEntity<Void> deleteApoderado(@PathVariable Long id) {
    apoderadoService.deleteApoderado(id);
    return ResponseEntity.noContent().build();
  }
}
