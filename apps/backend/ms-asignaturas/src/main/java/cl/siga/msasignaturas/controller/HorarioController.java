package cl.siga.msasignaturas.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import cl.siga.coreshare.dto.asignatura.horario.HorarioRequestDTO;
import cl.siga.coreshare.dto.asignatura.horario.HorarioResponseDTO;
import cl.siga.msasignaturas.service.HorarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/v1/horarios")
@RequiredArgsConstructor 
@Tag (name = "Horarios", description = "Operaciones exclusivas de registro, modificación y eliminación de horarios vinculados a una dictación")
public class HorarioController {

    private final HorarioService horarioService;

    @PostMapping("/curso-asignatura/{cursoAsignaturaId}")
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_horarios:write')")
    public ResponseEntity<HorarioResponseDTO> registrarHorario(
            @PathVariable Long cursoAsignaturaId,
            @RequestBody @Valid HorarioRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(horarioService.crearHorario(cursoAsignaturaId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_horarios:update')")
    public ResponseEntity<HorarioResponseDTO> updateHorario(
            @PathVariable Long id,
            @RequestBody @Valid HorarioRequestDTO request) {
        return ResponseEntity.ok(horarioService.actualizarHorario(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_horarios:delete')")
    public ResponseEntity<Void> deleteHorario(@PathVariable Long id) {
        horarioService.eliminarHorario(id);
        return ResponseEntity.noContent().build();
    }
}
