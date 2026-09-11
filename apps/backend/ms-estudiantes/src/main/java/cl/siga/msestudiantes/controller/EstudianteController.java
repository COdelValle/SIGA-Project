package cl.siga.msestudiantes.controller;

import cl.siga.coreshare.dto.estudiante.ActualizarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.estudiante.RegistrarEstudianteRequestDTO;

import java.time.LocalDate;
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

import cl.siga.coreshare.dto.estudiante.enums.State;
import cl.siga.msestudiantes.service.EstudianteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/api/v1/estudiantes")
@RequiredArgsConstructor 
public class EstudianteController {
    private final EstudianteService estudianteService;

    @GetMapping ("/{id}")
    @PreAuthorize ("hasAuthority('SCOPE_estudiantes:read')")
    public ResponseEntity<EstudianteResponseDTO> getEstudianteById(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.getEstudianteById(id));
    }

    @GetMapping ("/idUsuario/{idUsuario}")
    @PreAuthorize ("hasAuthority('SCOPE_estudiantes:read')")
    public ResponseEntity<EstudianteResponseDTO> getEstudianteByIdUsuario(@PathVariable String idUsuario) {
        return ResponseEntity.ok(estudianteService.getEstudianteByIdUsuario(idUsuario));
    }

    @GetMapping ("/search")
    @PreAuthorize ("hasAuthority('SCOPE_estudiantes:read')")
    public ResponseEntity<List<EstudianteResponseDTO>> searchEstudiantes(
            @RequestParam(required = false) String rut,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String middleName,
            @RequestParam(required = false) String firstSurname,
            @RequestParam(required = false) String secondSurname,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) State state
    ) {
        return ResponseEntity.ok(estudianteService.searchEstudiantes(rut, firstName, middleName, firstSurname, secondSurname, from, to, state));
    }

    @PostMapping 
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_estudiantes:write')")
    public ResponseEntity<EstudianteResponseDTO> registrarEstudiante(@Valid @RequestBody RegistrarEstudianteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estudianteService.saveEstudiante(request));
    }

    @PutMapping ("/{id}")
    @PreAuthorize ("hasRole('ADMIN') or (hasRole('APODERADO') and hasAuthority('SCOPE_estudiantes:update'))")
    public ResponseEntity<EstudianteResponseDTO> actualizarEstudiante(@PathVariable Long id, @Valid @RequestBody ActualizarEstudianteRequestDTO request) {
        return ResponseEntity.ok(estudianteService.updateEstudiante(id, request));
    }

    @GetMapping ("/exists/{id}")
    @PreAuthorize ("hasAuthority('SCOPE_estudiantes:read')")
    public ResponseEntity<Boolean> existsEstudianteById(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.existsEstudianteById(id));
    }

    @DeleteMapping ("/{id}")
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_estudiantes:delete')")
    public ResponseEntity<Void> deleteEstudiante(@PathVariable Long id) {
        estudianteService.deleteEstudiante(id);
        return ResponseEntity.noContent().build();
    }
}
