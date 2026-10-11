package cl.siga.bffweb.domain.evaluaciones;

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

import cl.siga.coreshare.dto.evaluaciones.ActualizarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.RegistrarEvaluacionRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bff/v1/evaluaciones")
@RequiredArgsConstructor
public class EvaluacionController {
    private final EvaluacionBffService service;

    /** Crea una evaluacion validando que la asignatura pertenezca al docente. */
    @PostMapping
    @PreAuthorize("(hasRole('ADMIN') or hasRole('DOCENTE')) and hasAuthority('SCOPE_evaluaciones:write')")
    public ResponseEntity<EvaluacionResponseDTO> crearEvaluacion(
            @Valid @RequestBody RegistrarEvaluacionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearEvaluacion(request));
    }

    /** Edita una evaluacion validando la pertenencia del docente. */
    @PutMapping("/{id}")
    @PreAuthorize("(hasRole('ADMIN') or hasRole('DOCENTE')) and hasAuthority('SCOPE_evaluaciones:update')")
    public ResponseEntity<EvaluacionResponseDTO> editarEvaluacion(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEvaluacionRequestDTO request) {
        return ResponseEntity.ok(service.editarEvaluacion(id, request));
    }

    /** Elimina logicamente una evaluacion validando la pertenencia del docente. */
    @DeleteMapping("/{id}")
    @PreAuthorize("(hasRole('ADMIN') or hasRole('DOCENTE')) and hasAuthority('SCOPE_evaluaciones:delete')")
    public ResponseEntity<Void> eliminarEvaluacion(@PathVariable Long id) {
        service.eliminarEvaluacion(id);
        return ResponseEntity.noContent().build();
    }
}
