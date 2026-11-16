package cl.siga.msusuariosauth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

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
import cl.siga.coreshare.dto.usuario.enums.RegistrationProcessState;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepState;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.msusuariosauth.service.AsyncUserRegistrationService;
import cl.siga.msusuariosauth.service.UsuarioService;

class UsuarioControllerTest {

    private UsuarioService usuarioService;
    private AsyncUserRegistrationService asyncService;
    private UsuarioController controller;

    @BeforeEach
    void setUp() {
        usuarioService = mock(UsuarioService.class);
        asyncService = mock(AsyncUserRegistrationService.class);
        controller = new UsuarioController(usuarioService, asyncService);
    }

    @Test
    void meDevuelveElUsuarioAutenticado() {
        UsuarioResponseDTO dto = new UsuarioResponseDTO("oid", "Ana", "ana@test.com", Rol.DOCENTE, StateUsuario.ACTIVO);
        when(usuarioService.getCurrentUsuario()).thenReturn(dto);

        assertThat(controller.me().getBody()).isEqualTo(dto);
    }

    @Test
    void endpointsDeConsultaDeleganEnElServicio() {
        CandidatoUsuarioResponseDTO candidato = new CandidatoUsuarioResponseDTO("oid", "ana@test.com", "Ana");
        UsuarioResponseDTO dto = new UsuarioResponseDTO("oid", "Ana", "ana@test.com", Rol.DOCENTE, StateUsuario.ACTIVO);
        Page<UsuarioResponseDTO> pagina = new PageImpl<>(List.of(dto));
        Pageable pageable = PageRequest.of(0, 20);
        when(usuarioService.lookupCandidato("ana@test.com")).thenReturn(candidato);
        when(usuarioService.getUsuarioById("oid")).thenReturn(dto);
        when(usuarioService.searchUsuarios("ana@test.com", Rol.DOCENTE, StateUsuario.ACTIVO, pageable))
                .thenReturn(pagina);

        assertThat(controller.lookup("ana@test.com").getBody()).isEqualTo(candidato);
        assertThat(controller.usuarioById("oid").getBody()).isEqualTo(dto);
        assertThat(controller.searchUsuarios("ana@test.com", Rol.DOCENTE, StateUsuario.ACTIVO, pageable)
                .getBody()).isEqualTo(pagina);
    }

    @Test
    void postUsuarioResponde202ParaInvitadoY201ParaActivo() {
        RegistrarUsuarioRequestDTO request = new RegistrarUsuarioRequestDTO("oid", "ana@test.com", Rol.DOCENTE);
        when(usuarioService.saveUsuario(request))
                .thenReturn(new RegistrarUsuarioResponseDTO(null, "ana@test.com", Rol.DOCENTE, StateUsuario.INVITADO))
                .thenReturn(new RegistrarUsuarioResponseDTO("oid", "ana@test.com", Rol.DOCENTE, StateUsuario.ACTIVO));

        ResponseEntity<RegistrarUsuarioResponseDTO> invitado = controller.postUsuario(request);
        ResponseEntity<RegistrarUsuarioResponseDTO> activo = controller.postUsuario(request);

        assertThat(invitado.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(activo.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void invitacionesPorLoteYCsvDeleganEnElServicio() {
        List<InvitacionUsuarioRequestDTO> solicitudes =
                List.of(new InvitacionUsuarioRequestDTO("ana@test.com", Rol.DOCENTE));
        InvitacionLoteResponseDTO respuesta = new InvitacionLoteResponseDTO(1, 0, List.of());
        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.csv", "text/csv", "a@b.cl".getBytes());
        when(usuarioService.invitarLote(solicitudes)).thenReturn(respuesta);
        when(usuarioService.invitarLoteCsv(archivo)).thenReturn(respuesta);

        assertThat(controller.postInvitacionesLote(solicitudes).getBody()).isEqualTo(respuesta);
        assertThat(controller.postInvitacionesLoteCsv(archivo).getBody()).isEqualTo(respuesta);
    }

    @Test
    void putDeleteYSyncRolesRespondenSegunCorresponde() {
        ActualizarUsuarioRequestDTO request =
                new ActualizarUsuarioRequestDTO("ana@test.com", StateUsuario.ACTIVO, Rol.DOCENTE);
        UsuarioResponseDTO dto = new UsuarioResponseDTO("oid", "Ana", "ana@test.com", Rol.DOCENTE, StateUsuario.ACTIVO);
        when(usuarioService.updateUsuario("oid", request)).thenReturn(dto);

        assertThat(controller.putUsuario("oid", request).getBody()).isEqualTo(dto);

        ResponseEntity<Void> eliminado = controller.deleteUsuario("oid");
        ResponseEntity<Void> sincronizado = controller.syncRoles("oid");

        assertThat(eliminado.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(sincronizado.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(usuarioService).deleteUsuario("oid");
        verify(usuarioService).syncUserRoles("oid");
    }

    @Test
    void endpointsDeRegistroAsincronoDeleganYRespondenAccepted() {
        RegistrarUsuarioCompuestoRequestDTO request = new RegistrarUsuarioCompuestoRequestDTO(
                "ana@test.com", "Ana", Rol.ESTUDIANTE, null, null, null);
        UserRegistrationStatusResponseDTO estado = new UserRegistrationStatusResponseDTO(
                "proc-1", RegistrationProcessState.EN_PROCESO, RegistrationStepState.PENDIENTE,
                RegistrationStepState.PENDIENTE, Rol.ESTUDIANTE, "ana@test.com", null, null, null, null);
        UserRegistrationCredentialResponseDTO credencial =
                new UserRegistrationCredentialResponseDTO("proc-1", "ana@test.com", "oid-1", "Temp", null);
        when(asyncService.startRegistration(request, "corr-1")).thenReturn(estado);
        when(asyncService.getStatus("proc-1")).thenReturn(estado);
        when(asyncService.obtenerCredencial("proc-1")).thenReturn(credencial);
        when(asyncService.resetPassword("oid-1")).thenReturn(credencial);

        ResponseEntity<UserRegistrationStatusResponseDTO> creado =
                controller.postRegistroCompuestoAsync(request, "corr-1");
        ResponseEntity<UserRegistrationStatusResponseDTO> consultado =
                controller.getRegistroCompuestoStatus("proc-1");
        ResponseEntity<UserRegistrationCredentialResponseDTO> obtenida =
                controller.getCredencialTemporal("proc-1");
        ResponseEntity<UserRegistrationCredentialResponseDTO> reseteada =
                controller.resetPassword("oid-1");

        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(creado.getBody()).isEqualTo(estado);
        assertThat(consultado.getBody()).isEqualTo(estado);
        assertThat(obtenida.getBody()).isEqualTo(credencial);
        assertThat(reseteada.getBody()).isEqualTo(credencial);
        verify(asyncService).startRegistration(eq(request), eq("corr-1"));
        verify(asyncService).getStatus("proc-1");
        verify(asyncService).obtenerCredencial("proc-1");
        verify(asyncService).resetPassword("oid-1");
    }
}
