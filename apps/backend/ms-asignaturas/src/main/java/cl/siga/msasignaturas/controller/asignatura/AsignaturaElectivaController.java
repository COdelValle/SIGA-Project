package cl.siga.msasignaturas.controller.asignatura;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.electiva.ActualizarAsignaturaElectivaRequestDTO;
import cl.siga.coreshare.dto.asignatura.electiva.RegistrarAsignaturaElectivaRequestDTO;
import cl.siga.msasignaturas.service.asignatura.AsignaturaElectivaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/v1/asignaturas/electivas")
@RequiredArgsConstructor 
@Tag (name = "Asignaturas Electivas", description = "Operaciones de registro, modificación y validación normativa de electivos")
public class AsignaturaElectivaController {

    private final AsignaturaElectivaService electivaService;

    @PostMapping
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:write')")
    public ResponseEntity<AsignaturaResponseDTO> registrarElectiva(
            @RequestBody @Valid RegistrarAsignaturaElectivaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(electivaService.crearElectiva(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:update')")
    public ResponseEntity<AsignaturaResponseDTO> updateElectiva(
            @PathVariable Long id,
            @RequestBody @Valid ActualizarAsignaturaElectivaRequestDTO request) {
        return ResponseEntity.ok(electivaService.actualizarElectiva(id, request));
    }

    @GetMapping("/validar-mineduc")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_asignaturas:read')")
    public ResponseEntity<Map<String, Object>> validarMineduc() {
        return ResponseEntity.ok(electivaService.validarCumplimientoMineduc());
    }
}