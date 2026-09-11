package cl.siga.msnotas.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.siga.coreshare.dto.notas.ActualizarNotaRequestDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.dto.notas.RegistrarNotaRequestDTO;
import cl.siga.msnotas.service.NotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/api/v1/notas")
@RequiredArgsConstructor 
public class NotaController {
    private final NotaService notaService;

    @GetMapping ("/{id}")
    @PreAuthorize ("hasAuthority('SCOPE_notas:read')")
    public ResponseEntity<NotaResponseDTO> getNotaById(@PathVariable Long id) {
        return ResponseEntity.ok(notaService.getNotaById(id));
    }

    @GetMapping ("/search")
    @PreAuthorize ("hasAuthority('SCOPE_notas:read')")
    public ResponseEntity<List<NotaResponseDTO>> searchNotas(
        @RequestParam (required = false) Long idEstudiante,
        @RequestParam (required = false) Long idAsignatura,
        @RequestParam (required = false) Double lessThatScore,
        @RequestParam (required = false) Double greaterThanScore
    ){
        return ResponseEntity.ok(notaService.searchNotas(idEstudiante, idAsignatura, lessThatScore, greaterThanScore));
    }

    @PostMapping
    @PreAuthorize ("hasRole('ADMIN') or (hasRole('DOCENTE') and hasAuthority('SCOPE_notas:write'))")
    public ResponseEntity<NotaResponseDTO> registrarNota(@RequestBody @Valid RegistrarNotaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notaService.saveNota(request));
    }

    @PutMapping ("/{id}")
    @PreAuthorize ("hasRole('ADMIN') or (hasRole('DOCENTE') and hasAuthority('SCOPE_notas:update'))")
    public ResponseEntity<NotaResponseDTO> updateNota(@PathVariable Long id, @RequestBody @Valid ActualizarNotaRequestDTO request) {
        return ResponseEntity.ok(notaService.updateNota(id, request));
    }

    @DeleteMapping ("/{id}")
    @PreAuthorize ("hasRole('ADMIN') or (hasRole('DOCENTE') and hasAuthority('SCOPE_notas:delete'))")
    public ResponseEntity<Void> deleteNota(@PathVariable Long id) {
        notaService.deleteNota(id);
        return ResponseEntity.noContent().build();
    }
}
