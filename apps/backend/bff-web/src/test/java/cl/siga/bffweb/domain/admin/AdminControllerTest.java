package cl.siga.bffweb.domain.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import cl.siga.bffweb.domain.admin.dto.api.UsuarioDetalleDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioCompuestoRequestDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationCredentialResponseDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStatusResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;

class AdminControllerTest {

    private final AdminBffService service = mock(AdminBffService.class);
    private final AdminController controller = new AdminController(service);

    @Test
    void listadoDeUsuariosDelegaConLosFiltros() {
        when(service.getUsuarios("ana", Rol.DOCENTE, StateUsuario.ACTIVO)).thenReturn(List.of());

        var respuesta = controller.getUsuarios("ana", Rol.DOCENTE, StateUsuario.ACTIVO);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).isEmpty();
        verify(service).getUsuarios("ana", Rol.DOCENTE, StateUsuario.ACTIVO);
    }

    @Test
    void listadoDeAsignaturasYDeMallaDeleganEnElServicio() {
        when(service.getAsignaturas()).thenReturn(List.of());
        when(service.getMalla(Nivel.OCTAVO_BASICO)).thenReturn(List.of());

        assertThat(controller.getAsignaturas().getBody()).isEmpty();
        assertThat(controller.getMalla(Nivel.OCTAVO_BASICO).getBody()).isEmpty();

        verify(service).getAsignaturas();
        verify(service).getMalla(Nivel.OCTAVO_BASICO);
    }

    @Test
    void registroCompuestoDevuelveAccepted() {
        RegistrarUsuarioCompuestoRequestDTO request = mock(RegistrarUsuarioCompuestoRequestDTO.class);
        UserRegistrationStatusResponseDTO estado = mock(UserRegistrationStatusResponseDTO.class);
        when(service.iniciarRegistroCompuesto(request)).thenReturn(estado);

        var respuesta = controller.iniciarRegistro(request);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(202);
        assertThat(respuesta.getBody()).isSameAs(estado);
        verify(service).iniciarRegistroCompuesto(request);
    }

    @Test
    void consultaDeRegistroYCredencialDeleganEnElServicio() {
        UserRegistrationStatusResponseDTO estado = mock(UserRegistrationStatusResponseDTO.class);
        UserRegistrationCredentialResponseDTO credencial = mock(UserRegistrationCredentialResponseDTO.class);
        when(service.getRegistroCompuesto("p-1")).thenReturn(estado);
        when(service.getCredencialTemporal("p-1")).thenReturn(credencial);

        assertThat(controller.getRegistro("p-1").getBody()).isSameAs(estado);
        assertThat(controller.getCredencial("p-1").getBody()).isSameAs(credencial);

        verify(service).getRegistroCompuesto("p-1");
        verify(service).getCredencialTemporal("p-1");
    }

    @Test
    void resetPasswordYDetalleDeUsuarioDeleganEnElServicio() {
        UserRegistrationCredentialResponseDTO credencial = mock(UserRegistrationCredentialResponseDTO.class);
        UsuarioDetalleDTO detalle = mock(UsuarioDetalleDTO.class);
        when(service.resetPassword("oid-1")).thenReturn(credencial);
        when(service.getUsuarioDetalle("oid-1")).thenReturn(detalle);

        assertThat(controller.resetPassword("oid-1").getBody()).isSameAs(credencial);
        assertThat(controller.getUsuarioDetalle("oid-1").getBody()).isSameAs(detalle);

        verify(service).resetPassword("oid-1");
        verify(service).getUsuarioDetalle("oid-1");
    }

    @Test
    void eliminarUsuarioResponde204() {
        var respuesta = controller.eliminarUsuario("oid-1");

        assertThat(respuesta.getStatusCode().value()).isEqualTo(204);
        verify(service).eliminarUsuario("oid-1");
    }

    @Test
    void selectoresDeClasesYEstudiantesDeleganEnElServicio() {
        when(service.getClases(2026)).thenReturn(List.of());
        when(service.buscarEstudiantes("ana")).thenReturn(List.of());

        assertThat(controller.getClases(2026).getBody()).isEmpty();
        assertThat(controller.buscarEstudiantes("ana").getBody()).isEmpty();

        verify(service).getClases(2026);
        verify(service).buscarEstudiantes("ana");
    }
}
