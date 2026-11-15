package cl.siga.bffweb.domain.estudiantes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import cl.siga.bffweb.domain.estudiantes.dto.api.PerfilEstudianteResponseDTO;

class PerfilEstudianteControllerTest {

    private final PerfilEstudianteService service = mock(PerfilEstudianteService.class);
    private final PerfilEstudianteController controller = new PerfilEstudianteController(service);

    @Test
    void perfilDelEstudianteAutenticadoDelegaEnElServicio() {
        PerfilEstudianteResponseDTO perfil = mock(PerfilEstudianteResponseDTO.class);
        when(service.getPerfilMe()).thenReturn(perfil);

        var respuesta = controller.getPerfilMe();

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).isSameAs(perfil);
        verify(service).getPerfilMe();
    }

    @Test
    void perfilPorIdentificadorDelegaEnElServicio() {
        PerfilEstudianteResponseDTO perfil = mock(PerfilEstudianteResponseDTO.class);
        when(service.getPerfil(7L)).thenReturn(perfil);

        var respuesta = controller.getPerfil(7L);

        assertThat(respuesta.getBody()).isSameAs(perfil);
        verify(service).getPerfil(7L);
    }
}
