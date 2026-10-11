package cl.siga.bffweb.domain.apoderados;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.siga.bffweb.domain.apoderados.dto.PupiloResumenDTO;
import cl.siga.coreshare.dto.estudiante.ActualizarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/bff/v1/apoderados")
@RequiredArgsConstructor 
@Tag(name = "Apoderados", description = "Pupilos del apoderado autenticado")
public class ApoderadoPupiloController {

    private final ApoderadoPupiloService service;

    @GetMapping("/pupilos")
    @PreAuthorize("hasRole('APODERADO') and hasAuthority('SCOPE_apoderados:read')")
    public ResponseEntity<List<PupiloResumenDTO>> getPupilos() {
        return ResponseEntity.ok(service.getPupilos());
    }

    @PutMapping("/pupilos/{idEstudiante}")
    @PreAuthorize("hasRole('APODERADO') and hasAuthority('SCOPE_estudiantes:update')")
    public ResponseEntity<EstudianteResponseDTO> actualizarPupilo(
            @PathVariable Long idEstudiante,
            @Valid @RequestBody ActualizarEstudianteRequestDTO request) {
        return ResponseEntity.ok(service.actualizarPupilo(idEstudiante, request));
    }
}
