package cl.siga.msusuariosauth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateInvitacion;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.msusuariosauth.integration.graph.GraphUserDirectory;
import cl.siga.msusuariosauth.model.entity.InvitacionUsuario;
import cl.siga.msusuariosauth.model.entity.Usuario;
import cl.siga.msusuariosauth.repository.InvitacionUsuarioRepository;
import cl.siga.msusuariosauth.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class InvitacionVinculacionServiceTest {

    private static final String OID = "12345678-1234-1234-1234-123456789012";
    private static final String EMAIL = "user@test.com";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private InvitacionUsuarioRepository invitacionRepository;

    @Mock
    private GraphUserDirectory graphDirectory;

    private InvitacionVinculacionService service;

    @BeforeEach
    void setUp() {
        service = new InvitacionVinculacionService(usuarioRepository, invitacionRepository, graphDirectory);
    }

    private InvitacionUsuario invitacion(StateInvitacion state) {
        return InvitacionUsuario.builder()
                .email(EMAIL)
                .rol(Rol.DOCENTE)
                .state(state)
                .build();
    }

    @Test
    void vincular_creaUsuarioYMarcaLasInvitacion() {
        InvitacionUsuario invitacion = invitacion(StateInvitacion.INVITADO);
        when(invitacionRepository.findByEmail(EMAIL)).thenReturn(Optional.of(invitacion));
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(graphDirectory.isEnabled()).thenReturn(false);

        Optional<Usuario> result = service.vincular(OID, EMAIL);

        assertTrue(result.isPresent());
        assertEquals(OID, result.get().getId());
        assertEquals(EMAIL, result.get().getEmail());
        assertEquals(Rol.DOCENTE, result.get().getRol());
        assertEquals(StateUsuario.ACTIVO, result.get().getState());
        assertEquals(StateInvitacion.VINCULADA, invitacion.getState());
        assertNotNull(invitacion.getBoundAt());
        verify(invitacionRepository).save(invitacion);
    }

    @Test
    void vincular_sinInvitacionNoCreaUsuario() {
        when(invitacionRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        Optional<Usuario> result = service.vincular(OID, EMAIL);

        assertFalse(result.isPresent());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void vincular_conInvitacionYaVinculadaNoHaceNada() {
        when(invitacionRepository.findByEmail(EMAIL)).thenReturn(Optional.of(invitacion(StateInvitacion.VINCULADA)));

        Optional<Usuario> result = service.vincular(OID, EMAIL);

        assertFalse(result.isPresent());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void vincular_conCorreoYaRegistradoNoReescribeLaCuenta() {
        when(invitacionRepository.findByEmail(EMAIL)).thenReturn(Optional.of(invitacion(StateInvitacion.INVITADO)));
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(true);

        Optional<Usuario> result = service.vincular(OID, EMAIL);

        assertFalse(result.isPresent());
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    void vincular_sinEmailNoConsultaNada() {
        Optional<Usuario> result = service.vincular(OID, "   ");

        assertFalse(result.isPresent());
        verify(invitacionRepository, never()).findByEmail(any());
    }
}
