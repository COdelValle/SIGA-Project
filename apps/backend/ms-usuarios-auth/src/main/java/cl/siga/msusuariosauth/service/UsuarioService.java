package cl.siga.msusuariosauth.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import cl.siga.coreshare.dto.usuario.ActualizarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msusuariosauth.model.entity.Usuario;
import cl.siga.msusuariosauth.model.mapper.UsuarioMapper;
import cl.siga.msusuariosauth.model.specification.UsuarioSpecifications;
import cl.siga.msusuariosauth.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@Validated 
@RequiredArgsConstructor 
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;

    private final UsuarioMapper usuarioMapper;

    @Transactional (readOnly = true)
    public UsuarioResponseDTO getUsuarioById(String id) {
        return usuarioMapper.toResponseDto(usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + id + " no encontrado.")));
    }

    @Transactional (readOnly = true)
    public List<UsuarioResponseDTO> searchUsuarios(String email, Rol rol, StateUsuario state) {
        Specification<Usuario> spec = (root, query, cb) -> cb.conjunction();

        if (email != null && !email.isBlank()) {
            spec = spec.and(UsuarioSpecifications.hasEmail(email));
        }
        if (rol != null) {
            spec = spec.and(UsuarioSpecifications.hasRol(rol));
        }
        if (state != null) {
            spec = spec.and(UsuarioSpecifications.hasState(state));
        }

        return usuarioMapper.toResponseDtoList(usuarioRepository.findAll(spec));
    }

    @Transactional
    public UsuarioResponseDTO saveUsuario(@Valid RegistrarUsuarioRequestDTO request) {
        if (usuarioRepository.existsById(request.id())) {
            throw new BusinessException("El usuario con ID de Azure " + request.id() + " ya está registrado.");
        }

        String emailFormatted = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(emailFormatted)) {
            throw new BusinessException("El correo electrónico ya está registrado.");
        }
        
        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setState(StateUsuario.ACTIVO);

        return usuarioMapper.toResponseDto(usuarioRepository.save(usuario));
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

        usuarioMapper.updateEntityFromDto(request, existingUsuario);

        return usuarioMapper.toResponseDto(usuarioRepository.save(existingUsuario));
    }

    @Transactional
    public void deleteUsuario(String id) {
        Usuario existingUsuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + id + " no encontrado."));
        existingUsuario.setState(StateUsuario.INACTIVO);
    }
}
