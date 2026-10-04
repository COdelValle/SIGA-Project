package cl.siga.bffweb.domain.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.siga.bffweb.domain.admin.dto.AsignaturaAdminDTO;
import cl.siga.bffweb.domain.admin.dto.UsuarioAdminDTO;
import cl.siga.coreshare.dto.asignatura.malla.MallaCurricularResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bff/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminBffService service;

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:read')")
    public ResponseEntity<List<UsuarioAdminDTO>> getUsuarios(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) StateUsuario state) {
        return ResponseEntity.ok(service.getUsuarios(email, rol, state));
    }

    @GetMapping("/asignaturas")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:read')")
    public ResponseEntity<List<AsignaturaAdminDTO>> getAsignaturas() {
        return ResponseEntity.ok(service.getAsignaturas());
    }

    @GetMapping("/malla")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_asignaturas:read')")
    public ResponseEntity<List<MallaCurricularResponseDTO>> getMalla(
            @RequestParam(required = false) Nivel nivel) {
        return ResponseEntity.ok(service.getMalla(nivel));
    }
}
