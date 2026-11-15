package cl.siga.bffweb.domain.me;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import cl.siga.bffweb.domain.me.dto.api.MeResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;

class MeControllerTest {

    private final MeService service = mock(MeService.class);
    private final MeController controller = new MeController(service);

    @Test
    void exponeElPerfilDelUsuarioAutenticado() {
        MeResponseDTO me = new MeResponseDTO("oid-1", "camila@siga.cl", "Camila", List.of(Rol.ESTUDIANTE));
        when(service.getMe()).thenReturn(me);

        var respuesta = controller.me();

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).isEqualTo(me);
        verify(service).getMe();
    }
}
