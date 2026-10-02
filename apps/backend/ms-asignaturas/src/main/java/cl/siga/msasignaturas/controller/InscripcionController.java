package cl.siga.msasignaturas.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import cl.siga.coreshare.dto.asignatura.inscripcion.ActualizarEstadoInscripcionRequestDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.RegistrarInscripcionRequestDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.msasignaturas.service.InscripcionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/v1/inscripciones")
@RequiredArgsConstructor 
@Tag (name = "Inscripciones", description = "Gestión de inscripciones a asignaturas electivas y búsqueda dinámica")
public class InscripcionController {

    private final InscripcionService inscripcionService;

    @Operation (summary = "Obtener una inscripción por su ID")
    @GetMapping("/{id}")
    @PreAuthorize ("hasAuthority('SCOPE_inscripciones:read')")
    public ResponseEntity<InscripcionResponseDTO> getInscripcionById(@PathVariable Long id) {
        return ResponseEntity.ok(inscripcionService.obtenerPorId(id));
    }

    @Operation(
        summary = "Buscar inscripciones dinámicamente", 
        description = "Filtra de forma opcional por idAlumno, idAsignatura y/o múltiples estados (ej. ?estados=ACTIVO,PRE_INSCRITO)"
    )
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SCOPE_inscripciones:read')")
    public ResponseEntity<List<InscripcionResponseDTO>> searchInscripciones(
            @RequestParam(required = false) Long idAlumno,
            @RequestParam(required = false) Long idAsignatura,
            @RequestParam(required = false) List<EstadoInscripcion> estados) {
        
        return ResponseEntity.ok(inscripcionService.buscarInscripciones(idAlumno, idAsignatura, estados));
    }

    @Operation(summary = "Registrar un alumno en una asignatura electiva")
    @PostMapping
    @PreAuthorize("(hasRole('ADMIN') or hasRole('ALUMNO')) and hasAuthority('SCOPE_inscripciones:write')")
    public ResponseEntity<InscripcionResponseDTO> registrar(
            @RequestBody @Valid RegistrarInscripcionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inscripcionService.registrarInscripcion(request));
    }

    @Operation(summary = "Actualizar exclusivamente el estado de una inscripción")
    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_inscripciones:update')")
    public ResponseEntity<InscripcionResponseDTO> actualizarEstado(
            @PathVariable Long id,
            @RequestBody @Valid ActualizarEstadoInscripcionRequestDTO request) {
        return ResponseEntity.ok(inscripcionService.actualizarEstado(id, request));
    }
}