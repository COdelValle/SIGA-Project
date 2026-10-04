package cl.siga.msasignaturas.controller.asignatura;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.curso.ActualizarCursoAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.curso.RegistrarCursoAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msasignaturas.service.asignatura.CursoAsignaturaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/curso-asignaturas")
@RequiredArgsConstructor
@Tag(name = "Dictaciones", description = "Asignaturas dictadas en un curso, con docente, horarios y cupos")
public class CursoAsignaturaController {

    private final CursoAsignaturaService cursoAsignaturaService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_asignaturas:read')")
    public ResponseEntity<CursoAsignaturaResponseDTO> getCursoAsignaturaById(@PathVariable Long id) {
        return ResponseEntity.ok(cursoAsignaturaService.obtenerPorId(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SCOPE_asignaturas:read')")
    public ResponseEntity<Page<CursoAsignaturaResponseDTO>> buscarCursoAsignaturas(
            @RequestParam(required = false) Long idClase,
            @RequestParam(required = false) Long idDocente,
            @RequestParam(required = false) Long idAsignatura,
            @RequestParam(required = false) CaracterAsignatura caracter,
            @RequestParam(required = false) Semestre semestre,
            @RequestParam(required = false) AreaAcademica area,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean conCupoDisponible,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {

        return ResponseEntity.ok(cursoAsignaturaService.searchCursoAsignaturas(
                idClase, idDocente, idAsignatura, caracter, semestre, area, nombre, conCupoDisponible, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:write')")
    public ResponseEntity<CursoAsignaturaResponseDTO> registrarCursoAsignatura(
            @RequestBody @Valid RegistrarCursoAsignaturaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cursoAsignaturaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:update')")
    public ResponseEntity<CursoAsignaturaResponseDTO> actualizarCursoAsignatura(
            @PathVariable Long id,
            @RequestBody @Valid ActualizarCursoAsignaturaRequestDTO request) {
        return ResponseEntity.ok(cursoAsignaturaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:delete')")
    public ResponseEntity<Void> deleteCursoAsignatura(@PathVariable Long id) {
        cursoAsignaturaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists/{id}")
    @PreAuthorize("hasAuthority('SCOPE_asignaturas:read')")
    public ResponseEntity<Boolean> existsCursoAsignaturaById(@PathVariable Long id) {
        return ResponseEntity.ok(cursoAsignaturaService.existCursoAsignatura(id));
    }

    @GetMapping("/validar-mineduc")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:read')")
    public ResponseEntity<Map<String, Object>> validarMineduc() {
        return ResponseEntity.ok(cursoAsignaturaService.validarCumplimientoMineduc());
    }
}
