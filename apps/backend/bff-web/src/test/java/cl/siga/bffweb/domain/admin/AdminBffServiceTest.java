package cl.siga.bffweb.domain.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.usuarios.UsuarioClient;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.enums.TipoAsignatura;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.enums.AreaAcademica;

class AdminBffServiceTest {

    private final UsuarioClient usuarioClient = mock(UsuarioClient.class);
    private final AsignaturaClient asignaturaClient = mock(AsignaturaClient.class);

    private final AdminBffService service = new AdminBffService(usuarioClient, asignaturaClient);

    @Test
    void getUsuariosMapeaRolYEstado() {
        when(usuarioClient.searchUsuarios(any(), any(), any(), anyInt())).thenReturn(pagina(List.of(
            new UsuarioResponseDTO("oid-1", "camila.soto@platformsiga.onmicrosoft.com",
                Rol.ESTUDIANTE, StateUsuario.ACTIVO))));

        var usuarios = service.getUsuarios(null, null, null);

        assertThat(usuarios).hasSize(1);
        assertThat(usuarios.get(0).email()).isEqualTo("camila.soto@platformsiga.onmicrosoft.com");
        assertThat(usuarios.get(0).rol()).isEqualTo("ESTUDIANTE");
        assertThat(usuarios.get(0).estado()).isEqualTo("ACTIVO");
    }

    @Test
    void getAsignaturasMapeaNombreYDescripcion() {
        when(asignaturaClient.searchAsignaturas(anyInt())).thenReturn(pagina(List.of(
            new AsignaturaResponseDTO(1L, "MATEMATICA", "matematica", Semestre.SEMESTRE_1,
                AreaAcademica.MATEMATICAS, TipoAsignatura.BASICA, 6L, List.of(), 4L, null, null, null, List.of()))));

        var asignaturas = service.getAsignaturas();

        assertThat(asignaturas).hasSize(1);
        assertThat(asignaturas.get(0).nombre()).isEqualTo("MATEMATICA");
        assertThat(asignaturas.get(0).activa()).isTrue();
    }

    private static <T> PageResponseDTO<T> pagina(List<T> content) {
        return new PageResponseDTO<>(content, content.size(), 1, 200, 0, true, true, content.isEmpty());
    }
}
