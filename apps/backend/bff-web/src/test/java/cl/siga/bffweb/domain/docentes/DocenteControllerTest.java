package cl.siga.bffweb.domain.docentes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import cl.siga.bffweb.domain.notas.NotaBffService;
import cl.siga.bffweb.domain.notas.dto.CursoNotasDTO;

class DocenteControllerTest {

    private final DocenteBffService service = mock(DocenteBffService.class);
    private final NotaBffService notaService = mock(NotaBffService.class);
    private final DocenteController controller = new DocenteController(service, notaService);

    @Test
    void cursosDelDocenteDeleganEnElServicio() {
        when(service.getCursos()).thenReturn(List.of());

        var respuesta = controller.getCursos();

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).isEmpty();
        verify(service).getCursos();
    }

    @Test
    void horarioDelDocenteDelegaEnElServicio() {
        when(service.getHorario()).thenReturn(List.of());

        var respuesta = controller.getHorario();

        assertThat(respuesta.getBody()).isEmpty();
        verify(service).getHorario();
    }

    @Test
    void notasDelCursoDeleganEnElServicioDeNotas() {
        CursoNotasDTO curso = mock(CursoNotasDTO.class);
        when(notaService.getCursoNotas(3L)).thenReturn(curso);

        var respuesta = controller.getCursoNotas(3L);

        assertThat(respuesta.getBody()).isSameAs(curso);
        verify(notaService).getCursoNotas(3L);
    }
}
