package cl.siga.msasistencias.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.siga.coreshare.dto.asistencia.ActualizarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.asistencia.RegistrarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.asistencia.enums.State;
import cl.siga.msasistencias.service.AsistenciaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/asistencias")
@RequiredArgsConstructor
@Tag(name = "Asistencias", description = "Registro y consulta de asistencias por estudiante y dictación")
public class AsistenciaController {
    private final AsistenciaService asistenciaService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_asistencias:read')")
    public ResponseEntity<AsistenciaResponseDTO> getAsistenciaById(@PathVariable Long id) {
        return ResponseEntity.ok(asistenciaService.getAsistenciaById(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SCOPE_asistencias:read')")
    public ResponseEntity<Page<AsistenciaResponseDTO>> searchAsistencias(
            @RequestParam(required = false) Long idEstudiante,
            @RequestParam(required = false) Long idCursoAsignatura,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) State estado,
            @PageableDefault(size = 20, sort = "fecha") Pageable pageable) {
        return ResponseEntity.ok(asistenciaService.searchAsistencias(
            idEstudiante, idCursoAsignatura, from, to, estado, pageable));
    }

    @PostMapping
    @PreAuthorize("(hasRole('ADMIN') or hasRole('DOCENTE')) and hasAuthority('SCOPE_asistencias:write')")
    public ResponseEntity<AsistenciaResponseDTO> registrarAsistencia(
            @Valid @RequestBody RegistrarAsistenciaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asistenciaService.saveAsistencia(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("(hasRole('ADMIN') or hasRole('DOCENTE')) and hasAuthority('SCOPE_asistencias:update')")
    public ResponseEntity<AsistenciaResponseDTO> actualizarAsistencia(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarAsistenciaRequestDTO request) {
        return ResponseEntity.ok(asistenciaService.updateAsistencia(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asistencias:delete')")
    public ResponseEntity<Void> deleteAsistencia(@PathVariable Long id) {
        asistenciaService.deleteAsistencia(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists/{id}")
    @PreAuthorize("hasAuthority('SCOPE_asistencias:read')")
    public ResponseEntity<Boolean> existsAsistenciaById(@PathVariable Long id) {
        return ResponseEntity.ok(asistenciaService.existsAsistenciaById(id));
    }
}
