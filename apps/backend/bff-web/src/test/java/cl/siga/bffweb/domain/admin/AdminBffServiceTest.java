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
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.PlanFormacion;
import cl.siga.coreshare.dto.asignatura.malla.MallaCurricularResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
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
    void getAsignaturasMapeaCatalogoConNiveles() {
        when(asignaturaClient.searchAsignaturas(anyInt())).thenReturn(pagina(List.of(
            new AsignaturaResponseDTO(1L, "Matemática", "Matemática", "matemática",
                AreaAcademica.MATEMATICAS, true, true))));
        when(asignaturaClient.getMalla(null)).thenReturn(List.of(
            new MallaCurricularResponseDTO(1L, Nivel.OCTAVO_BASICO, 1L, "Matemática",
                AreaAcademica.MATEMATICAS, CaracterAsignatura.OBLIGATORIA, PlanFormacion.COMUN,
                null, true, true),
            new MallaCurricularResponseDTO(2L, Nivel.PRIMERO_BASICO, 1L, "Matemática",
                AreaAcademica.MATEMATICAS, CaracterAsignatura.OBLIGATORIA, PlanFormacion.COMUN,
                null, true, true)));

        var asignaturas = service.getAsignaturas();

        assertThat(asignaturas).hasSize(1);
        assertThat(asignaturas.get(0).nombre()).isEqualTo("Matemática");
        assertThat(asignaturas.get(0).area()).isEqualTo(AreaAcademica.MATEMATICAS);
        assertThat(asignaturas.get(0).calificable()).isTrue();
        assertThat(asignaturas.get(0).niveles()).containsExactly("1ro Básico", "8vo Básico");
        assertThat(asignaturas.get(0).activa()).isTrue();
    }

    private static <T> PageResponseDTO<T> pagina(List<T> content) {
        return new PageResponseDTO<>(content, content.size(), 1, 200, 0, true, true, content.isEmpty());
    }
}
