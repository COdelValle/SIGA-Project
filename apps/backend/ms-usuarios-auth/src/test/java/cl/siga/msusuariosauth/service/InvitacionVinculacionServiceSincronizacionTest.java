package cl.siga.msusuariosauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateInvitacion;
import cl.siga.msusuariosauth.integration.graph.GraphUserDirectory;
import cl.siga.msusuariosauth.model.entity.InvitacionUsuario;
import cl.siga.msusuariosauth.model.entity.Usuario;
import cl.siga.msusuariosauth.repository.InvitacionUsuarioRepository;
import cl.siga.msusuariosauth.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvitacionVinculacionServiceSincronizacionTest {

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
        InvitacionUsuario invitacion = InvitacionUsuario.builder()
                .email(EMAIL).rol(Rol.DOCENTE).state(StateInvitacion.INVITADO).build();
        when(invitacionRepository.findByEmail(EMAIL)).thenReturn(Optional.of(invitacion));
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(graphDirectory.isEnabled()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sinTransaccionActivaSincronizaElRolDeInmediato() {
        service.vincular(OID, EMAIL);

        verify(graphDirectory).syncRole(OID, Rol.DOCENTE);
    }

    @Test
    void conTransaccionActivaSincronizaElRolDespuesDelCommit() {
        TransactionSynchronizationManager.initSynchronization();

        service.vincular(OID, EMAIL);

        assertThat(TransactionSynchronizationManager.getSynchronizations()).isNotEmpty();
        verify(graphDirectory, org.mockito.Mockito.never()).syncRole(any(), any());

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(graphDirectory).syncRole(OID, Rol.DOCENTE);
    }

    @Test
    void unFalloDeGraphDespuesDelCommitNoSePropaga() {
        doThrow(new IllegalStateException("Graph caido"))
                .when(graphDirectory).syncRole(OID, Rol.DOCENTE);
        TransactionSynchronizationManager.initSynchronization();

        service.vincular(OID, EMAIL);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(graphDirectory).syncRole(OID, Rol.DOCENTE);
    }
}
