package cl.siga.msdocentes.controller;

import cl.siga.coreshare.dto.docente.ActualizarDocenteRequestDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.dto.docente.RegistrarDocenteRequestDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msdocentes.service.DocenteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/v1/docentes")
@RequiredArgsConstructor
@Tag(name = "Docentes", description = "Operaciones CRUD y busqueda de docentes")
public class DocenteController {
  private final DocenteService docenteService;

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('SCOPE_docentes:read')")
  public ResponseEntity<DocenteResponseDTO> getDocenteById(@PathVariable Long id) {
    return ResponseEntity.ok(docenteService.getDocenteById(id));
  }

  @GetMapping("/idUsuario/{idUsuario}")
  @PreAuthorize("hasAuthority('SCOPE_docentes:read')")
  public ResponseEntity<DocenteResponseDTO> getDocenteByIdUsuario(@PathVariable String idUsuario) {
    return ResponseEntity.ok(docenteService.getDocenteByIdUsuario(idUsuario));
  }

  @GetMapping("/search")
  @PreAuthorize("hasAuthority('SCOPE_docentes:read')")
  public ResponseEntity<Page<DocenteResponseDTO>> searchDocentes(
    @RequestParam(required = false) String rut,
    @RequestParam(required = false) String firstName,
    @RequestParam(required = false) String firstSurname,
    @RequestParam(required = false) LocalDate from,
    @RequestParam(required = false) LocalDate to,
    @RequestParam(required = false) AreaAcademica area,
    @PageableDefault(size = 20, sort = "id") Pageable pageable
  ) {
    return ResponseEntity.ok(docenteService.searchDocentes(rut, firstName, firstSurname, from, to, area, pageable));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_docentes:write')")
  public ResponseEntity<DocenteResponseDTO> registrarDocente(@Valid @RequestBody RegistrarDocenteRequestDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(docenteService.saveDocente(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("(hasRole('ADMIN') or hasRole('DOCENTE')) and hasAuthority('SCOPE_docentes:update')")
  public ResponseEntity<DocenteResponseDTO> actualizarDocente(@PathVariable Long id, @Valid @RequestBody ActualizarDocenteRequestDTO request) {
    return ResponseEntity.ok(docenteService.updateDocente(id, request));
  }

  @GetMapping("/exists/{id}")
  @PreAuthorize("hasAuthority('SCOPE_docentes:read')")
  public ResponseEntity<Boolean> existsDocenteById(@PathVariable Long id) {
    return ResponseEntity.ok(docenteService.existsDocenteById(id));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_docentes:delete')")
  public ResponseEntity<Void> deleteDocente(@PathVariable Long id) {
    docenteService.deleteDocente(id);
    return ResponseEntity.noContent().build();
  }
}
