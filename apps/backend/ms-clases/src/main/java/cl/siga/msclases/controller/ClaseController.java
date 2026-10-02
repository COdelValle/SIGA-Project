package cl.siga.msclases.controller;

import cl.siga.coreshare.dto.clase.ActualizarDocenteJefeRequestDTO;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.clase.RegistrarClaseRequestDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.msclases.service.ClaseService;
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
@RequestMapping("/api/v1/clases")
@RequiredArgsConstructor
@Tag(name = "Clases", description = "Operaciones de registro, asignación y consulta de clases (cursos)")
public class ClaseController {
  private final ClaseService claseService;

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('SCOPE_clases:read')")
  public ResponseEntity<ClaseResponseDTO> getClaseById(@PathVariable Long id) {
    return ResponseEntity.ok(claseService.getClaseById(id));
  }

  @GetMapping("/search")
  @PreAuthorize("hasAuthority('SCOPE_clases:read')")
  public ResponseEntity<Page<ClaseResponseDTO>> searchClases(
    @RequestParam(required = false) Nivel nivel,
    @RequestParam(required = false) String letra,
    @RequestParam(required = false) Integer anioAcademico,
    @RequestParam(required = false) Long idDocenteJefe,
    @PageableDefault(size = 20, sort = "id") Pageable pageable
  ) {
    return ResponseEntity.ok(claseService.searchClases(nivel, letra, anioAcademico, idDocenteJefe, pageable));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_clases:write')")
  public ResponseEntity<ClaseResponseDTO> registrarClase(@RequestBody @Valid RegistrarClaseRequestDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(claseService.saveClase(request));
  }

  // Endpoint específico para actualizar el profesor jefe de un curso
  @PutMapping("/{id}/docente-jefe")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_clases:update')")
  public ResponseEntity<ClaseResponseDTO> actualizarDocenteJefe(
    @PathVariable Long id,
    @RequestBody @Valid ActualizarDocenteJefeRequestDTO request
  ) {
    return ResponseEntity.ok(claseService.updateDocenteJefe(id, request));
  }

  @GetMapping("/exists/{id}")
  @PreAuthorize("hasAuthority('SCOPE_clases:read')")
  public ResponseEntity<Boolean> existsClaseById(@PathVariable Long id) {
    return ResponseEntity.ok(claseService.existsClaseById(id));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_clases:delete')")
  public ResponseEntity<Void> deleteClase(@PathVariable Long id) {
    claseService.deleteClase(id);
    return ResponseEntity.noContent().build();
  }
}
