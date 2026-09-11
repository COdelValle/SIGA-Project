package cl.siga.msusuariosauth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.siga.coreshare.dto.usuario.ActualizarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.msusuariosauth.service.UsuarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;

@RestController 
@RequestMapping ("/api/v1/usuarios")
@RequiredArgsConstructor 
@Tag (name = "Usuarios", description = "Operaciones CRUD para la gestión de usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;

    @GetMapping("/{id}")
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:read')")
    public ResponseEntity<UsuarioResponseDTO> usuarioById(@PathVariable String id) {
        return ResponseEntity.ok(usuarioService.getUsuarioById(id));
    }
    
    @GetMapping("/search")
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:read')")
    public ResponseEntity<List<UsuarioResponseDTO>> searchUsuarios(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) StateUsuario state
        ) {
        return ResponseEntity.ok(usuarioService.searchUsuarios(email, rol, state));
    }
    
    @PostMapping()
    @PreAuthorize ("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:write')")
    public ResponseEntity<UsuarioResponseDTO> postUsuario(@RequestBody @Valid RegistrarUsuarioRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.saveUsuario(request));
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
}
