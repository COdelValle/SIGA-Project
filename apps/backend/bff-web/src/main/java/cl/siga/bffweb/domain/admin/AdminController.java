package cl.siga.bffweb.domain.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.siga.bffweb.domain.admin.dto.AsignaturaAdminDTO;
import cl.siga.bffweb.domain.admin.dto.UsuarioAdminDTO;
import cl.siga.bffweb.domain.admin.dto.api.ClaseOpcionDTO;
import cl.siga.bffweb.domain.admin.dto.api.EstudianteOpcionDTO;
import cl.siga.bffweb.domain.admin.dto.api.UsuarioDetalleDTO;
import cl.siga.coreshare.dto.asignatura.malla.MallaCurricularResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioCompuestoRequestDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationCredentialResponseDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStatusResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import jakarta.validation.Valid;
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

    // --- Registro asíncrono de usuarios (usuario + perfil de rol) ---

    @PostMapping("/registraciones")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:write')")
    public ResponseEntity<UserRegistrationStatusResponseDTO> iniciarRegistro(
            @RequestBody @Valid RegistrarUsuarioCompuestoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(service.iniciarRegistroCompuesto(request));
    }

    @GetMapping("/registraciones/{processId}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:read')")
    public ResponseEntity<UserRegistrationStatusResponseDTO> getRegistro(@PathVariable String processId) {
        return ResponseEntity.ok(service.getRegistroCompuesto(processId));
    }

    @GetMapping("/registraciones/{processId}/credencial")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:read')")
    public ResponseEntity<UserRegistrationCredentialResponseDTO> getCredencial(
            @PathVariable String processId) {
        return ResponseEntity.ok(service.getCredencialTemporal(processId));
    }

    @PostMapping("/usuarios/{idUsuario}/reset-password")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:update')")
    public ResponseEntity<UserRegistrationCredentialResponseDTO> resetPassword(
            @PathVariable String idUsuario) {
        return ResponseEntity.ok(service.resetPassword(idUsuario));
    }

    @GetMapping("/usuarios/{idUsuario}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:read')")
    public ResponseEntity<UsuarioDetalleDTO> getUsuarioDetalle(@PathVariable String idUsuario) {
        return ResponseEntity.ok(service.getUsuarioDetalle(idUsuario));
    }

    @DeleteMapping("/usuarios/{idUsuario}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:delete')")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable String idUsuario) {
        service.eliminarUsuario(idUsuario);
        return ResponseEntity.noContent().build();
    }

    // --- Selectores del registro de usuarios ---

    @GetMapping("/clases")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_clases:read')")
    public ResponseEntity<List<ClaseOpcionDTO>> getClases(
            @RequestParam(required = false) Integer anioAcademico) {
        return ResponseEntity.ok(service.getClases(anioAcademico));
    }

    @GetMapping("/estudiantes")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_estudiantes:read')")
    public ResponseEntity<List<EstudianteOpcionDTO>> buscarEstudiantes(@RequestParam String q) {
        return ResponseEntity.ok(service.buscarEstudiantes(q));
    }
}
