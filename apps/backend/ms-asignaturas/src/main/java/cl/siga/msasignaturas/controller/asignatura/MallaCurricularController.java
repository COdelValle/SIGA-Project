package cl.siga.msasignaturas.controller.asignatura;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import cl.siga.coreshare.dto.asignatura.enums.PlanFormacion;
import cl.siga.coreshare.dto.asignatura.malla.ActualizarMallaCurricularRequestDTO;
import cl.siga.coreshare.dto.asignatura.malla.MallaCurricularResponseDTO;
import cl.siga.coreshare.dto.asignatura.malla.RegistrarMallaCurricularRequestDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.msasignaturas.service.asignatura.MallaCurricularService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/malla")
@RequiredArgsConstructor
@Tag(name = "Malla curricular", description = "Asignaturas que aplican a cada nivel, con su carácter y horas")
public class MallaCurricularController {

    private final MallaCurricularService mallaCurricularService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_asignaturas:read')")
    public ResponseEntity<List<MallaCurricularResponseDTO>> listarMalla(
            @RequestParam(required = false) Nivel nivel,
            @RequestParam(required = false) PlanFormacion plan) {
        return ResponseEntity.ok(mallaCurricularService.listar(nivel, plan));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:write')")
    public ResponseEntity<MallaCurricularResponseDTO> registrarMalla(
            @RequestBody @Valid RegistrarMallaCurricularRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mallaCurricularService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:update')")
    public ResponseEntity<MallaCurricularResponseDTO> actualizarMalla(
            @PathVariable Long id,
            @RequestBody @Valid ActualizarMallaCurricularRequestDTO request) {
        return ResponseEntity.ok(mallaCurricularService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:delete')")
    public ResponseEntity<Void> deleteMalla(@PathVariable Long id) {
        mallaCurricularService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
