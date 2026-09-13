package cl.siga.msusuariosauth.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import cl.siga.coreshare.dto.usuario.ActualizarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.CandidatoUsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.security.SecurityUtils;
import cl.siga.msusuariosauth.integration.graph.GraphUserDirectory;
import cl.siga.msusuariosauth.model.entity.Usuario;
import cl.siga.msusuariosauth.model.mapper.UsuarioMapper;
import cl.siga.msusuariosauth.model.specification.UsuarioSpecifications;
import cl.siga.msusuariosauth.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
@Validated 
@RequiredArgsConstructor 
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;

    private final UsuarioMapper mapper;

    private final GraphUserDirectory graphDirectory;

    @Transactional (readOnly = true)
    public UsuarioResponseDTO getUsuarioById(String id) {
        return mapper.toResponseDto(usuarioRepository.findById(id)
                .filter(usuario -> usuario.getState() != StateUsuario.INACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + id + " no encontrado.")));
    }

    /**
     * Usuario autenticado según el JWT (claim {@code oid}). Es la base de
     * {@code GET /api/v1/usuarios/me} que consume el BFF.
     */
    @Transactional (readOnly = true)
    public UsuarioResponseDTO getCurrentUsuario() {
        String oid = SecurityUtils.getCurrentUserOid()
                .orElseThrow(() -> new BusinessException("No se pudo determinar el usuario autenticado."));
        return getUsuarioById(oid);
    }

    @Transactional (readOnly = true)
    public List<UsuarioResponseDTO> searchUsuarios(@Valid @Email String email, Rol rol, StateUsuario state) {
        Specification<Usuario> spec = (root, query, cb) -> cb.notEqual(root.get("state"), StateUsuario.INACTIVO);

        if (email != null && !email.isBlank()) {
            spec = spec.and(UsuarioSpecifications.hasEmail(email));
        }
        if (rol != null) {
            spec = spec.and(UsuarioSpecifications.hasRol(rol));
        }
        if (state != null) {
            spec = spec.and(UsuarioSpecifications.hasState(state));
        }

        return mapper.toResponseDtoList(usuarioRepository.findAll(spec));
    }

    /** Busca una cuenta en Entra ID por correo para pre-registrarla. */
    @Transactional (readOnly = true)
    public CandidatoUsuarioResponseDTO lookupCandidato(@Valid @Email String email) {
        return graphDirectory.findUserByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró en Entra ID una cuenta para el correo " + email + "."));
    }

    @Transactional
    public UsuarioResponseDTO saveUsuario(@Valid RegistrarUsuarioRequestDTO request) {
        String emailFormatted = request.email().trim().toLowerCase();

        String oid = request.id();
        if (oid == null || oid.isBlank()) {
            oid = graphDirectory.findUserByEmail(emailFormatted)
                    .map(CandidatoUsuarioResponseDTO::oid)
                    .orElseThrow(() -> new BusinessException(
                            "No se encontró en Entra ID una cuenta para " + request.email() + "."));
        }

        if (usuarioRepository.existsById(oid)) {
            throw new BusinessException("El usuario con ID de Azure " + oid + " ya está registrado.");
        }
        if (usuarioRepository.existsByEmail(emailFormatted)) {
            throw new BusinessException("El correo electrónico ya está registrado.");
        }

        Usuario usuario = mapper.toEntity(request);
        usuario.setId(oid);
        usuario.setState(StateUsuario.ACTIVO);

        Usuario saved = usuarioRepository.save(usuario);
        syncRolesIfEnabled(saved);
        return mapper.toResponseDto(saved);
    }

    @Transactional
    public UsuarioResponseDTO updateUsuario(String id, @Valid ActualizarUsuarioRequestDTO request) {
        Usuario existingUsuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + id + " no encontrado."));

        String newEmailFormatted = request.email().trim().toLowerCase();

        // Validar si cambió el correo y si el nuevo correo pertenece a otro usuario
        if (!existingUsuario.getEmail().equalsIgnoreCase(newEmailFormatted)) {
            if (usuarioRepository.existsByEmail(newEmailFormatted)) {
                throw new BusinessException("El correo electrónico " + request.email() + " ya está en uso.");
            }
        }

        Rol previousRol = existingUsuario.getRol();
        StateUsuario previousState = existingUsuario.getState();
        mapper.updateEntityFromDto(request, existingUsuario);
        if (request.rol() != null) {
            existingUsuario.setRol(request.rol());
        }

        Usuario saved = usuarioRepository.save(existingUsuario);
        boolean rolChanged = previousRol != saved.getRol();
        boolean stateChanged = previousState != saved.getState();
        if (rolChanged || stateChanged) {
            if (rolChanged) {
                log.info("Rol del usuario {} cambió de {} a {}; sincronizando con Entra ID.",
                        saved.getId(), previousRol, saved.getRol());
            }
            syncRolesIfEnabled(saved);
        }
        return mapper.toResponseDto(saved);
    }

    @Transactional
    public void deleteUsuario(String id) {
        Usuario existingUsuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + id + " no encontrado."));
        existingUsuario.setState(StateUsuario.INACTIVO);
        usuarioRepository.save(existingUsuario);

        if (graphDirectory.isEnabled()) {
            graphDirectory.revokeRoles(existingUsuario.getId());
        }
    }

    /**
     * Fuerza la reconciliación del rol local hacia Entra ID. Útil para el
     * bootstrap del primer admin o si una sincronización previa falló.
     */
    @Transactional
    public void syncUserRoles(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + id + " no encontrado."));
        if (!graphDirectory.isEnabled()) {
            throw new BusinessException(
                    "La integración con Microsoft Graph no está configurada (falta AZURE_CLIENT_SECRET).");
        }
        syncRolesIfEnabled(usuario);
    }

    /**
     * Refleja en Entra ID el rol/estado local. La BD es la fuente autoritativa;
     * si Graph no está configurado (sin AZURE_CLIENT_SECRET) se omite sin romper
     * el CRUD.
     */
    private void syncRolesIfEnabled(Usuario usuario) {
        if (!graphDirectory.isEnabled()) {
            log.debug("Graph no configurado: se omite la sincronización de roles para {}", usuario.getId());
            return;
        }
        if (usuario.getState() == StateUsuario.INACTIVO) {
            graphDirectory.revokeRoles(usuario.getId());
        } else {
            graphDirectory.syncRole(usuario.getId(), usuario.getRol());
        }
    }
}
