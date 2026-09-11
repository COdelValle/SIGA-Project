package cl.siga.msasignaturas.controller;

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

import cl.siga.coreshare.dto.asignatura.AsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.msasignaturas.service.AsignaturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/api/v1/asignaturas")
@RequiredArgsConstructor 
public class AsignaturaController {
    private final AsignaturaService asignaturaService;

    @GetMapping ("/{id}")
    @PreAuthorize ("hasScope('asignaturas:read')")
    public ResponseEntity<AsignaturaResponseDTO> getAsignaturaById(@PathVariable Long id) {
        return ResponseEntity.ok(asignaturaService.getAsignaturaById(id));
    }

    @GetMapping ("/name/{name}")
    @PreAuthorize ("hasScope('asignaturas:read')")
    public ResponseEntity<AsignaturaResponseDTO> getAsignaturaByName(@PathVariable String name) {
        return ResponseEntity.ok(asignaturaService.getAsignaturaByName(name));
    }

    @PostMapping 
    @PreAuthorize ("hasRole('ADMIN') and hasScope('asignaturas:write')")
    public ResponseEntity<AsignaturaResponseDTO> saveAsignatura( @Valid @RequestBody AsignaturaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asignaturaService.saveAsignatura(request));
    }

    @PutMapping ("/{id}")
    @PreAuthorize ("hasRole('ADMIN') and hasScope('asignaturas:update')")
    public ResponseEntity<AsignaturaResponseDTO> updateAsignatura(@PathVariable Long id, @Valid @RequestBody AsignaturaRequestDTO request) {
        return ResponseEntity.ok(asignaturaService.updateAsignatura(id, request));
    }
}
