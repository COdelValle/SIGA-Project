package cl.siga.msasignaturas.controller.asignatura;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.enums.TipoAsignatura;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msasignaturas.service.asignatura.AsignaturaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/asignaturas")
@RequiredArgsConstructor
@Tag(name = "Asignaturas Generales", description = "Operaciones de consulta global y eliminación de asignaturas")
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
            @RequestParam(required = false) String name,
            @RequestParam(required = false) TipoAsignatura tipo,
            @RequestParam(required = false) Semestre semestre,
            @RequestParam(required = false) AreaAcademica area,
            @RequestParam(required = false) Long idDocente,
            @RequestParam(required = false) Long idClase,
            @RequestParam(required = false) Boolean verificarCupos,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        
        return ResponseEntity.ok(asignaturaService.searchAsignaturas(
                name, tipo, semestre, area, idDocente, idClase, verificarCupos, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:delete')")
    public ResponseEntity<Void> deleteAsignatura(@PathVariable Long id) {
        asignaturaService.eliminarAsignatura(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists/{id}")
    @PreAuthorize("hasAuthority('SCOPE_asignaturas:read')")
    public ResponseEntity<Boolean> existsAsignaturaById(@PathVariable Long id) {
        return ResponseEntity.ok(asignaturaService.existAsignatura(id));
    }
}