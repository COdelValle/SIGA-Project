package cl.siga.msdocentes.controller;

import cl.siga.coreshare.dto.docente.certificado.CertificadoRequestDTO;
import cl.siga.coreshare.dto.docente.certificado.CertificadoResponseDTO;
import cl.siga.msdocentes.service.CertificadoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/docentes/{docenteId}/certificados")
@RequiredArgsConstructor
@Tag(name = "Certificados Docente", description = "Gestión independiente de certificados por docente")
public class CertificadoController {
  private final CertificadoService certificadoService;

  @GetMapping
  @PreAuthorize("hasAuthority('SCOPE_docentes:read')")
  public ResponseEntity<List<CertificadoResponseDTO>> getCertificados(@PathVariable Long docenteId) {
    return ResponseEntity.ok(certificadoService.getCertificadosByDocenteId(docenteId));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_docentes:write')")
  public ResponseEntity<CertificadoResponseDTO> addCertificado(
    @PathVariable Long docenteId,
    @Valid @RequestBody CertificadoRequestDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(certificadoService.addCertificado(docenteId, request));
  }

  @DeleteMapping("/{certificadoId}")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_docentes:delete')")
  public ResponseEntity<Void> deleteCertificado(
    @PathVariable Long docenteId,
    @PathVariable Long certificadoId) {
    certificadoService.deleteCertificado(docenteId, certificadoId);
    return ResponseEntity.noContent().build();
  }
}
