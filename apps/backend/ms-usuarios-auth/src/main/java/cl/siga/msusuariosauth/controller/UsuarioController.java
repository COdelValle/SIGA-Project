package cl.siga.msusuariosauth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.siga.coreshare.dto.usuario.ActualizarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.CandidatoUsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.InvitacionLoteResponseDTO;
import cl.siga.coreshare.dto.usuario.InvitacionUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioCompuestoRequestDTO;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationCredentialResponseDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStatusResponseDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.msusuariosauth.service.UsuarioService;
import cl.siga.msusuariosauth.service.AsyncUserRegistrationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.multipart.MultipartFile;

@RestController 
@RequestMapping ("/api/v1/usuarios")
@RequiredArgsConstructor 
@Tag (name = "Usuarios", description = "Operaciones CRUD para la gestión de usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;
    private final AsyncUserRegistrationService asyncUserRegistrationService;

    @GetMapping("/me")
    @PreAuthorize ("isAuthenticated()")
    public ResponseEntity<UsuarioResponseDTO> me() {
        return ResponseEntity.ok(usuarioService.getCurrentUsuario());
    }

    @GetMapping("/lookup")
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:read')")
    public ResponseEntity<CandidatoUsuarioResponseDTO> lookup(@RequestParam @Email String email) {
        return ResponseEntity.ok(usuarioService.lookupCandidato(email));
    }

    @GetMapping("/{id}")
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:read')")
    public ResponseEntity<UsuarioResponseDTO> usuarioById(@PathVariable String id) {
        return ResponseEntity.ok(usuarioService.getUsuarioById(id));
    }
    
    @GetMapping("/search")
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:read')")
    public ResponseEntity<Page<UsuarioResponseDTO>> searchUsuarios(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) StateUsuario state,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
        ) {
        return ResponseEntity.ok(usuarioService.searchUsuarios(email, rol, state, pageable));
    }
    
    @PostMapping()
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:write')")
    public ResponseEntity<RegistrarUsuarioResponseDTO> postUsuario(@RequestBody @Valid RegistrarUsuarioRequestDTO request) {
        RegistrarUsuarioResponseDTO response = usuarioService.saveUsuario(request);
        HttpStatus status = response.state() == StateUsuario.INVITADO
                ? HttpStatus.ACCEPTED
                : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/invitaciones/lote")
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:write')")
    public ResponseEntity<InvitacionLoteResponseDTO> postInvitacionesLote(
            @RequestBody List<InvitacionUsuarioRequestDTO> solicitudes) {
        return ResponseEntity.ok(usuarioService.invitarLote(solicitudes));
    }

    @PostMapping(value = "/invitaciones/lote/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:write')")
    public ResponseEntity<InvitacionLoteResponseDTO> postInvitacionesLoteCsv(
            @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(usuarioService.invitarLoteCsv(archivo));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:update')")
    public ResponseEntity<UsuarioResponseDTO> putUsuario(@PathVariable String id, @RequestBody @Valid ActualizarUsuarioRequestDTO request) {
        return ResponseEntity.ok(usuarioService.updateUsuario(id, request));
    }

    @DeleteMapping ("/{id}")
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:delete')")
    public ResponseEntity<Void> deleteUsuario(@PathVariable String id) {
        usuarioService.deleteUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping ("/{id}/sync-roles")
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:update')")
    public ResponseEntity<Void> syncRoles(@PathVariable String id) {
        usuarioService.syncUserRoles(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/registraciones/async")
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:write')")
    public ResponseEntity<UserRegistrationStatusResponseDTO> postRegistroCompuestoAsync(
            @RequestBody @Valid RegistrarUsuarioCompuestoRequestDTO request,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId) {
        UserRegistrationStatusResponseDTO response = asyncUserRegistrationService.startRegistration(request, correlationId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/registraciones/{processId}")
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:read')")
    public ResponseEntity<UserRegistrationStatusResponseDTO> getRegistroCompuestoStatus(@PathVariable String processId) {
        return ResponseEntity.ok(asyncUserRegistrationService.getStatus(processId));
    }

    @GetMapping("/registraciones/{processId}/credencial")
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:read')")
    public ResponseEntity<UserRegistrationCredentialResponseDTO> getCredencialTemporal(@PathVariable String processId) {
        return ResponseEntity.ok(asyncUserRegistrationService.obtenerCredencial(processId));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:update')")
    public ResponseEntity<UserRegistrationCredentialResponseDTO> resetPassword(@PathVariable String id) {
        return ResponseEntity.ok(asyncUserRegistrationService.resetPassword(id));
    }
}
