package cl.siga.bffweb.domain.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.siga.bffweb.domain.admin.dto.api.ClaseOpcionDTO;
import cl.siga.bffweb.domain.admin.dto.api.EstudianteOpcionDTO;
import cl.siga.bffweb.integration.apoderados.ApoderadoClient;
import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.clases.ClaseClient;
import cl.siga.bffweb.integration.docentes.DocenteClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.bffweb.integration.usuarios.UsuarioClient;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class AdminBffServiceTest {

    @Mock
    private UsuarioClient usuarioClient;
    @Mock
    private AsignaturaClient asignaturaClient;
    @Mock
    private ClaseClient claseClient;
    @Mock
    private EstudianteClient estudianteClient;
    @Mock
    private DocenteClient docenteClient;
    @Mock
    private ApoderadoClient apoderadoClient;

    private AdminBffService service;

    @BeforeEach
    void setUp() {
        service = new AdminBffService(usuarioClient, asignaturaClient, claseClient,
                estudianteClient, docenteClient, apoderadoClient);
    }

    private static <T> PageResponseDTO<T> pagina(List<T> content) {
        return new PageResponseDTO<>(content, content.size(), 1, content.size(), 0, true, true, content.isEmpty());
    }

    @Test
    void getClasesMapeaNivelYAnio() {
        when(claseClient.searchClases(2026, 200)).thenReturn(pagina(List.of(
                new ClaseResponseDTO(1L, Nivel.PRIMERO_BASICO, "A", 2026, null))));

        List<ClaseOpcionDTO> clases = service.getClases(2026);

        assertEquals(1, clases.size());
        assertEquals("1ro Básico", clases.get(0).nivel());
        assertEquals("A", clases.get(0).letra());
        assertEquals(2026, clases.get(0).anioAcademico());
    }

    @Test
    void buscarEstudiantesExigeMinimoDosCaracteres() {
        assertTrue(service.buscarEstudiantes("c").isEmpty());

        verifyNoInteractions(estudianteClient);
    }

    @Test
    void buscarEstudiantesMapeaOpcionYLimpiaElTexto() {
        when(estudianteClient.searchEstudiantes("22.126.386", 20)).thenReturn(pagina(List.of(
                new EstudianteResponseDTO(7L, "oid-1", "22126386-3", "Catalina", null,
                        "Ormeño", null, null, null, "REGISTRADO", 1L))));

        List<EstudianteOpcionDTO> resultado = service.buscarEstudiantes("  22.126.386 ");

        assertEquals(1, resultado.size());
        assertEquals("22126386-3", resultado.get(0).rut());
        assertEquals("Catalina", resultado.get(0).firstName());
        assertEquals("Ormeño", resultado.get(0).firstSurname());
    }

    @Test
    void eliminarUsuarioDelegaEnElCliente() {
        service.eliminarUsuario("oid-1");

        verify(usuarioClient).deleteUsuario("oid-1");
    }

    @Test
    void getUsuariosUsaElNombreYSinNombreComoFallback() {
        when(usuarioClient.searchUsuarios(null, null, null, 200)).thenReturn(pagina(List.of(
                new UsuarioResponseDTO("oid-1", "Catalina Ormeño", "catalina@x", Rol.ESTUDIANTE, StateUsuario.ACTIVO),
                new UsuarioResponseDTO("oid-2", null, "sin.nombre@x", Rol.DOCENTE, StateUsuario.ACTIVO))));

        var usuarios = service.getUsuarios(null, null, null);

        assertEquals("Catalina Ormeño", usuarios.get(0).nombre());
        assertEquals("Sin nombre", usuarios.get(1).nombre());
    }

    @Test
    void getUsuarioDetalleDeEstudianteIncluyeClaseYAlergias() {
        when(usuarioClient.getUsuarioById("oid-1")).thenReturn(
                new UsuarioResponseDTO("oid-1", "Catalina Ormeño", "catalina@x", Rol.ESTUDIANTE, StateUsuario.ACTIVO));
        when(estudianteClient.getEstudianteByIdUsuario("oid-1")).thenReturn(
                new EstudianteResponseDTO(7L, "oid-1", "22126386-3", "CATALINA", null, "ORMEÑO", null,
                        LocalDate.of(2012, 5, 1), List.of("Maní"), "REGISTRADO", 1L));
        when(claseClient.getClaseById(1L)).thenReturn(
                new ClaseResponseDTO(1L, Nivel.PRIMERO_BASICO, "A", 2026, null));

        var detalle = service.getUsuarioDetalle("oid-1");

        assertEquals("Catalina Ormeño", detalle.fullName());
        assertEquals("22126386-3", detalle.rut());
        assertEquals("2012-05-01", detalle.fechaNacimiento());
        assertTrue(detalle.detalle().contains("1ro Básico A"));
        assertEquals(1, detalle.etiquetas().size());
    }

    @Test
    void getUsuarioDetalleSinPerfilNoFalla() {
        when(usuarioClient.getUsuarioById("oid-2")).thenReturn(
                new UsuarioResponseDTO("oid-2", null, "sin.perfil@x", Rol.DOCENTE, StateUsuario.INACTIVO));
        when(docenteClient.getDocenteByIdUsuario("oid-2"))
                .thenThrow(new ResourceNotFoundException("sin perfil"));

        var detalle = service.getUsuarioDetalle("oid-2");

        assertEquals("sin.perfil@x", detalle.fullName());
        assertTrue(detalle.detalle().toLowerCase().contains("sin perfil"));
    }
}
