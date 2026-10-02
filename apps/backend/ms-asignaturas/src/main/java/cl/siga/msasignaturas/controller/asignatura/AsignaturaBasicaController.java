package cl.siga.msasignaturas.controller.asignatura;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.basica.ActualizarAsignaturaBasicaRequestDTO;
import cl.siga.coreshare.dto.asignatura.basica.RegistrarAsignaturaBasicaRequestDTO;
import cl.siga.msasignaturas.service.asignatura.AsignaturaBasicaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/v1/asignaturas/basicas")
@RequiredArgsConstructor 
@Tag (name = "Asignaturas Básicas", description = "Operaciones de registro y modificación de asignaturas obligatorias por clase")
public class AsignaturaBasicaController {

    private final AsignaturaBasicaService basicaService;

    @PostMapping
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:write')")
    public ResponseEntity<AsignaturaResponseDTO> registrarBasica(
            @RequestBody @Valid RegistrarAsignaturaBasicaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(basicaService.crearBasica(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:update')")
    public ResponseEntity<AsignaturaResponseDTO> updateBasica(
            @PathVariable Long id,
            @RequestBody @Valid ActualizarAsignaturaBasicaRequestDTO request) {
        return ResponseEntity.ok(basicaService.actualizarBasica(id, request));
    }
}