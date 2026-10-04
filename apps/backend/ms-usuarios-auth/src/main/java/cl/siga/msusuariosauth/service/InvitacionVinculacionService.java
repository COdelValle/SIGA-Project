package cl.siga.msusuariosauth.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cl.siga.coreshare.dto.usuario.enums.StateInvitacion;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.msusuariosauth.integration.graph.GraphUserDirectory;
import cl.siga.msusuariosauth.model.entity.InvitacionUsuario;
import cl.siga.msusuariosauth.model.entity.Usuario;
import cl.siga.msusuariosauth.repository.InvitacionUsuarioRepository;
import cl.siga.msusuariosauth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Vincula una invitación por correo con el {@code oid} del token en el primer
 * inicio de sesión. Usa una transacción propia ({@code REQUIRES_NEW}) para que
 * un choque de concurrencia no invalide la transacción que atiende el request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitacionVinculacionService {

    private final UsuarioRepository usuarioRepository;
    private final InvitacionUsuarioRepository invitacionRepository;
    private final GraphUserDirectory graphDirectory;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Usuario> vincular(String oid, String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        InvitacionUsuario invitacion = invitacionRepository.findByEmail(email)
                .filter(inv -> inv.getState() == StateInvitacion.INVITADO)
                .orElse(null);
        if (invitacion == null) {
            return Optional.empty();
        }
        // Protege las referencias cruzadas por oid de otros servicios: si el
        // correo ya pertenece a otra cuenta, no se reescribe ni se duplica.
        if (usuarioRepository.existsByEmail(email)) {
            log.error("La invitación {} no se puede vincular: el correo ya pertenece a otro usuario.", email);
            return Optional.empty();
        }

        Usuario usuario = Usuario.builder()
                .id(oid)
                .email(email)
                .rol(invitacion.getRol())
                .state(StateUsuario.ACTIVO)
                .build();
        Usuario saved = usuarioRepository.saveAndFlush(usuario);

        invitacion.setState(StateInvitacion.VINCULADA);
        invitacion.setBoundAt(LocalDateTime.now());
        invitacionRepository.save(invitacion);

        syncRoleAfterCommit(saved);
        log.info("Usuario {} vinculado desde la invitación {} (rol {}).", oid, email, saved.getRol());
        return Optional.of(saved);
    }

    private void syncRoleAfterCommit(Usuario usuario) {
        if (!graphDirectory.isEnabled()) {
            log.debug("Graph no configurado: se omite la sincronización de rol para {}", usuario.getId());
            return;
        }
        runAfterCommit(() -> graphDirectory.syncRole(usuario.getId(), usuario.getRol()));
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        action.run();
                    } catch (RuntimeException ex) {
                        log.error("Fallo al sincronizar el rol en Entra ID: {}", ex.getMessage());
                    }
                }
            });
        } else {
            action.run();
        }
    }
}
