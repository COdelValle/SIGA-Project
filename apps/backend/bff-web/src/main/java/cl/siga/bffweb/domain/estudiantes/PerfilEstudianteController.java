package cl.siga.bffweb.domain.estudiantes;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.siga.bffweb.domain.estudiantes.dto.api.PerfilEstudianteResponseDTO;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/api/bff/v1/estudiantes")
@RequiredArgsConstructor
@PreAuthorize ("hasAuthority('SCOPE_estudiantes:read') and hasAuthority('SCOPE_asignaturas:read') and hasAuthority('SCOPE_notas:read')")
public class PerfilEstudianteController {
    private final PerfilEstudianteService service;

    /** Perfil del estudiante autenticado (resuelve el oid del token). */
    @GetMapping ("/perfil/me")
    public ResponseEntity<PerfilEstudianteResponseDTO> getPerfilMe() {
        return ResponseEntity.ok(service.getPerfilMe());
    }

    @GetMapping ("/perfil/{id}")
    public ResponseEntity<PerfilEstudianteResponseDTO> getPerfil(@PathVariable ("id") Long id) {
        return ResponseEntity.ok(service.getPerfil(id));
    }
}
