package cl.siga.bffweb.domain.asistencias;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.siga.coreshare.dto.asistencia.ActualizarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.asistencia.RegistrarAsistenciaRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bff/v1/asistencias")
@RequiredArgsConstructor
public class AsistenciaController {
    private final AsistenciaBffService service;

    /** Asistencias del estudiante autenticado. */
    @GetMapping("/estudiante/me")
    @PreAuthorize("hasAuthority('SCOPE_asistencias:read')")
    public ResponseEntity<List<AsistenciaResponseDTO>> getAsistenciasMe() {
        return ResponseEntity.ok(service.getAsistenciasMe());
    }

    /** Asistencias de un estudiante (propio, pupilo vinculado o docente/admin). */
    @GetMapping("/estudiante/{id}")
    @PreAuthorize("hasAuthority('SCOPE_asistencias:read')")
    public ResponseEntity<List<AsistenciaResponseDTO>> getAsistenciasEstudiante(@PathVariable Long id) {
        return ResponseEntity.ok(service.getAsistenciasEstudiante(id));
    }

    @PostMapping
    @PreAuthorize("(hasRole('ADMIN') or hasRole('DOCENTE')) and hasAuthority('SCOPE_asistencias:write')")
    public ResponseEntity<AsistenciaResponseDTO> registrarAsistencia(
            @Valid @RequestBody RegistrarAsistenciaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarAsistencia(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("(hasRole('ADMIN') or hasRole('DOCENTE')) and hasAuthority('SCOPE_asistencias:update')")
    public ResponseEntity<AsistenciaResponseDTO> actualizarAsistencia(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarAsistenciaRequestDTO request) {
        return ResponseEntity.ok(service.actualizarAsistencia(id, request));
    }
}
