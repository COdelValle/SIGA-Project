package cl.siga.bffweb.domain.notas;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.siga.coreshare.dto.notas.ActualizarNotaRequestDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.dto.notas.RegistrarNotaRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bff/v1/notas")
@RequiredArgsConstructor
public class NotaController {
    private final NotaBffService service;

    /** Crea una nota validando que la evaluacion pertenezca al docente autenticado. */
    @PostMapping
    @PreAuthorize("(hasRole('ADMIN') or hasRole('DOCENTE')) and hasAuthority('SCOPE_notas:write')")
    public ResponseEntity<NotaResponseDTO> crearNota(@Valid @RequestBody RegistrarNotaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearNota(request));
    }

    /** Edita el score de una nota validando la pertenencia del docente. */
    @PutMapping("/{id}")
    @PreAuthorize("(hasRole('ADMIN') or hasRole('DOCENTE')) and hasAuthority('SCOPE_notas:update')")
    public ResponseEntity<NotaResponseDTO> editarNota(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarNotaRequestDTO request) {
        return ResponseEntity.ok(service.editarNota(id, request));
    }

    /** Elimina logicamente una nota validando la pertenencia del docente. */
    @DeleteMapping("/{id}")
    @PreAuthorize("(hasRole('ADMIN') or hasRole('DOCENTE')) and hasAuthority('SCOPE_notas:delete')")
    public ResponseEntity<Void> eliminarNota(@PathVariable Long id) {
        service.eliminarNota(id);
        return ResponseEntity.noContent().build();
    }
}
