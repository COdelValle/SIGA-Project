package cl.siga.msasignaturas.controller.asignatura;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import cl.siga.coreshare.dto.asignatura.ActualizarAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.RegistrarAsignaturaRequestDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msasignaturas.service.asignatura.AsignaturaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/asignaturas")
@RequiredArgsConstructor
@Tag(name = "Asignaturas", description = "Catálogo general de asignaturas (independiente de cursos y docentes)")
public class AsignaturaController {

    private final AsignaturaService asignaturaService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_asignaturas:read')")
    public ResponseEntity<AsignaturaResponseDTO> getAsignaturaById(@PathVariable Long id) {
        return ResponseEntity.ok(asignaturaService.obtenerPorId(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SCOPE_asignaturas:read')")
    public ResponseEntity<Page<AsignaturaResponseDTO>> buscarAsignaturas(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) AreaAcademica area,
            @RequestParam(required = false) Boolean calificable,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {

        return ResponseEntity.ok(asignaturaService.searchAsignaturas(nombre, area, calificable, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:write')")
    public ResponseEntity<AsignaturaResponseDTO> registrarAsignatura(
            @RequestBody @Valid RegistrarAsignaturaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asignaturaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:update')")
    public ResponseEntity<AsignaturaResponseDTO> actualizarAsignatura(
            @PathVariable Long id,
            @RequestBody @Valid ActualizarAsignaturaRequestDTO request) {
        return ResponseEntity.ok(asignaturaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:delete')")
    public ResponseEntity<Void> deleteAsignatura(@PathVariable Long id) {
        asignaturaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists/{id}")
    @PreAuthorize("hasAuthority('SCOPE_asignaturas:read')")
    public ResponseEntity<Boolean> existsAsignaturaById(@PathVariable Long id) {
        return ResponseEntity.ok(asignaturaService.existAsignatura(id));
    }
}
