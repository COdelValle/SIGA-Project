package cl.siga.bffweb.domain.docentes;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.siga.bffweb.domain.docentes.dto.ClaseDocenteDTO;
import cl.siga.bffweb.domain.docentes.dto.CursoDocenteDTO;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bff/v1/docentes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCENTE') and hasAuthority('SCOPE_docentes:read')")
public class DocenteController {
    private final DocenteBffService service;

    /** Cursos del docente autenticado con sus alumnos. */
    @GetMapping("/cursos")
    public ResponseEntity<List<CursoDocenteDTO>> getCursos() {
        return ResponseEntity.ok(service.getCursos());
    }

    /** Horario semanal del docente autenticado. */
    @GetMapping("/horario")
    public ResponseEntity<List<ClaseDocenteDTO>> getHorario() {
        return ResponseEntity.ok(service.getHorario());
    }
}
