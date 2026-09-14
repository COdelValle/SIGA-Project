package cl.siga.msusuariosauth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.siga.coreshare.dto.usuario.RegistrarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msusuariosauth.integration.graph.GraphUserDirectory;
import cl.siga.msusuariosauth.model.entity.Usuario;
import cl.siga.msusuariosauth.model.mapper.UsuarioMapper;
import cl.siga.msusuariosauth.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String OID = "12345678-1234-1234-1234-123456789012";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioMapper mapper;

    @Mock
    private GraphUserDirectory graphDirectory;

    @InjectMocks
    private UsuarioService service;

    @Test
    void saveUsuario_registraComoActivo() {
        RegistrarUsuarioRequestDTO request =
                new RegistrarUsuarioRequestDTO(OID, "user@test.com", Rol.DOCENTE);
        Usuario entity = Usuario.builder().id(OID).email("user@test.com").rol(Rol.DOCENTE).build();
        UsuarioResponseDTO response =
                new UsuarioResponseDTO(OID, "user@test.com", Rol.DOCENTE, StateUsuario.ACTIVO);

        when(usuarioRepository.existsById(OID)).thenReturn(false);
        when(usuarioRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(usuarioRepository.save(entity)).thenReturn(entity);
        when(mapper.toResponseDto(entity)).thenReturn(response);

        UsuarioResponseDTO result = service.saveUsuario(request);

        assertNotNull(result);
        assertEquals(StateUsuario.ACTIVO, entity.getState());
        verify(usuarioRepository).save(entity);
    }

    @Test
    void saveUsuario_rechazaEmailDuplicado() {
        RegistrarUsuarioRequestDTO request =
                new RegistrarUsuarioRequestDTO(OID, "user@test.com", Rol.DOCENTE);

        when(usuarioRepository.existsById(OID)).thenReturn(false);
        when(usuarioRepository.existsByEmail("user@test.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.saveUsuario(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void getUsuarioById_lanzaCuandoNoExiste() {
        when(usuarioRepository.findById(OID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getUsuarioById(OID));
    }
}
